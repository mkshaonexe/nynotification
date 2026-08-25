package com.quietinbox.feature.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.quietinbox.core.apps.InstalledApp
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.firewall.FirewallAction
import com.quietinbox.core.health.HealthSnapshot
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import com.quietinbox.core.model.SignalClass
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.NotificationEntity
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.feature.inbox.data.InboxDao
import com.quietinbox.feature.inbox.data.NotificationWithVerdict
import com.quietinbox.feature.inbox.model.InboxItem
import com.quietinbox.feature.inbox.model.InboxStateFilter
import com.quietinbox.feature.inbox.util.InboxSearchHelper
import com.quietinbox.feature.inbox.util.InboxTimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * UI State holding user messages / snackbars for notifications.
 */
sealed interface InboxUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : InboxUiEvent
    data class CopyToClipboard(val code: String) : InboxUiEvent
}

/**
 * ViewModel managing search, filtering, selection, paging, and actions for the Inbox screen.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxDao: InboxDao,
    private val installedAppsProvider: InstalledAppsProvider,
    private val settingsDataStore: SettingsDataStore,
    private val listenerHealth: ListenerHealth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Initial arguments from navigation deep links (e.g. from heatmap or app list)
    private val initialPackage: String? = savedStateHandle.get<String>("packageName")
    private val initialDay: Int? = savedStateHandle.get<Int>("day")

    val searchQuery = MutableStateFlow("")
    val stateFilter = MutableStateFlow(InboxStateFilter.ALL)
    val selectedPackage = MutableStateFlow<String?>(initialPackage)
    val selectedDay = MutableStateFlow<Int?>(initialDay)

    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val detailNotification = MutableStateFlow<NotificationWithVerdict?>(null)

    private val _uiEvents = MutableSharedFlow<InboxUiEvent>()
    val uiEvents: SharedFlow<InboxUiEvent> = _uiEvents.asSharedFlow()

    // Undo delete buffer holding deleted entities
    private val undoDeleteBuffer = mutableListOf<NotificationEntity>()

    val installedApps: StateFlow<List<InstalledApp>> = MutableStateFlow<List<InstalledApp>>(emptyList()).apply {
        viewModelScope.launch {
            try {
                value = installedAppsProvider.all(includeSystemComponents = false)
            } catch (_: Exception) {
                value = emptyList()
            }
        }
    }.asStateFlow()

    val healthSnapshot: StateFlow<HealthSnapshot> = listenerHealth.snapshot

    val totalNotificationCount: StateFlow<Int> = inboxDao.observeNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val showTransportInInbox: StateFlow<Boolean> = settingsDataStore.settings
        .map { it.showTransportInInbox }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Debounced search query (250ms)
    private val debouncedSearchQuery: Flow<String> = searchQuery
        .debounce(250)
        .distinctUntilChanged()

    /**
     * Combined PagingData flow providing items with day header separators.
     */
    val pagedInboxItems: Flow<PagingData<InboxItem>> = combine(
        debouncedSearchQuery,
        stateFilter,
        selectedPackage,
        selectedDay,
        showTransportInInbox
    ) { query, state, pkg, day, showTransport ->
        FilterParams(
            query = query.trim(),
            stateFilter = state,
            packageName = pkg,
            day = day,
            showTransport = showTransport
        )
    }.flatMapLatest { params ->
        val digitsQuery = InboxSearchHelper.extractDigitsQuery(params.query)
        val dayEpochRange = params.day?.let { InboxTimeHelper.dayNumberToEpochRange(it) }

        Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
                initialLoadSize = 60
            )
        ) {
            inboxDao.pagingSource(
                stateFilter = params.stateFilter.name,
                packageName = params.packageName,
                startEpochMs = dayEpochRange?.first,
                endEpochMs = dayEpochRange?.second,
                includeTransport = params.showTransport,
                query = params.query.ifEmpty { null },
                digitsQuery = digitsQuery
            )
        }.flow.map { pagingData ->
            pagingData.map { entity ->
                mapToInboxRow(entity)
            }.insertSeparators { before: InboxItem.Row?, after: InboxItem.Row? ->
                val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
                val beforeDay = before?.let { InboxTimeHelper.toEpochDay(it.firstSeenAt) }
                val afterDay = after?.let { InboxTimeHelper.toEpochDay(it.firstSeenAt) }

                if (after != null && beforeDay != afterDay) {
                    InboxItem.Header(
                        title = InboxTimeHelper.formatDayHeader(
                            epochMs = after.firstSeenAt,
                            todayEpochDay = todayEpochDay
                        ),
                        epochDay = afterDay ?: 0L
                    )
                } else {
                    null
                }
            }
        }
    }.cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onStateFilterChanged(filter: InboxStateFilter) {
        stateFilter.value = filter
    }

    fun onPackageFilterChanged(pkg: String?) {
        selectedPackage.value = pkg
    }

    fun onDayFilterChanged(day: Int?) {
        selectedDay.value = day
    }

    fun clearFilters() {
        searchQuery.value = ""
        stateFilter.value = InboxStateFilter.ALL
        selectedPackage.value = null
        selectedDay.value = null
    }

    fun toggleStar(id: Long, currentStarred: Boolean) {
        viewModelScope.launch {
            inboxDao.setStarred(id, !currentStarred)
        }
    }

    fun setSeen(id: Long, isSeen: Boolean) {
        viewModelScope.launch {
            inboxDao.setSeen(id, isSeen)
        }
    }

    fun markAllSeen() {
        viewModelScope.launch {
            inboxDao.markAllSeen()
        }
    }

    fun toggleSelect(id: Long) {
        val current = selectedIds.value
        selectedIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAll(ids: List<Long>) {
        selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun markSelectedSeen(isSeen: Boolean) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            inboxDao.setSeenByIds(ids, isSeen)
            clearSelection()
        }
    }

    fun toggleStarSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            inboxDao.setStarredByIds(ids, true)
            clearSelection()
        }
    }

    fun deleteNotification(row: InboxItem.Row) {
        viewModelScope.launch {
            val entity = inboxDao.getNotificationById(row.id)
            if (entity != null) {
                undoDeleteBuffer.clear()
                undoDeleteBuffer.add(entity)
                inboxDao.deleteById(row.id)
                _uiEvents.emit(
                    InboxUiEvent.ShowSnackbar(
                        message = "Notification deleted",
                        actionLabel = "Undo",
                        onAction = { undoDelete() }
                    )
                )
            }
        }
    }

    fun deleteSelected(rows: List<InboxItem.Row>) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val entities = ids.mapNotNull { inboxDao.getNotificationById(it) }
            if (entities.isNotEmpty()) {
                undoDeleteBuffer.clear()
                undoDeleteBuffer.addAll(entities)
                inboxDao.deleteByIds(ids)
                clearSelection()
                val msg = if (ids.size == 1) "1 notification deleted" else "${ids.size} notifications deleted"
                _uiEvents.emit(
                    InboxUiEvent.ShowSnackbar(
                        message = msg,
                        actionLabel = "Undo",
                        onAction = { undoDelete() }
                    )
                )
            }
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            if (undoDeleteBuffer.isNotEmpty()) {
                inboxDao.insertNotifications(undoDeleteBuffer.toList())
                undoDeleteBuffer.clear()
            }
        }
    }

    fun openDetail(row: InboxItem.Row) {
        viewModelScope.launch {
            // Opening marks it seen
            if (!row.isSeen) {
                inboxDao.setSeen(row.id, true)
            }
            detailNotification.value = NotificationWithVerdict(
                id = row.id,
                sbnKey = row.sbnKey,
                packageName = row.packageName,
                appLabel = row.appLabel,
                title = row.title,
                body = row.body,
                subText = row.subText,
                senderName = row.senderName,
                senderDigits = row.senderDigits,
                channelId = row.channelId,
                androidCategory = row.androidCategory,
                importance = row.importance ?: 0,
                signalClass = row.signalClass.name,
                contentHash = row.contentHash,
                firstSeenAt = row.firstSeenAt,
                lastSeenAt = row.lastSeenAt,
                endedAt = row.endedAt,
                updateCount = row.updateCount,
                wasRateLimited = row.wasRateLimited,
                isSeen = true,
                isStarred = row.isStarred,
                removalReason = row.removalReason,
                firewallAction = row.firewallAction?.name,
                firewallRuleId = row.firewallRuleId,
                firewallRuleLabel = row.firewallRuleLabel
            )
        }
    }

    fun closeDetail() {
        detailNotification.value = null
    }

    fun muteApp(packageName: String) {
        viewModelScope.launch {
            inboxDao.muteApp(
                MutedAppEntity(
                    packageName = packageName,
                    mutedAt = System.currentTimeMillis()
                )
            )
            _uiEvents.emit(InboxUiEvent.ShowSnackbar("App added to muted apps"))
        }
    }

    fun muteSelectedApps(selectedRows: List<InboxItem.Row>) {
        viewModelScope.launch {
            val distinctPackages = selectedRows.map { it.packageName }.distinct()
            distinctPackages.forEach { pkg ->
                inboxDao.muteApp(
                    MutedAppEntity(
                        packageName = pkg,
                        mutedAt = System.currentTimeMillis()
                    )
                )
            }
            clearSelection()
            _uiEvents.emit(InboxUiEvent.ShowSnackbar("Selected apps muted"))
        }
    }

    fun alwaysAllowSender(senderName: String?, senderDigits: String?, packageName: String) {
        viewModelScope.launch {
            val rule = if (!senderName.isNullOrBlank()) {
                AllowRuleEntity(
                    type = "SENDER",
                    value = senderName,
                    matchMode = "CONTAINS",
                    enabled = true,
                    createdAt = System.currentTimeMillis()
                )
            } else if (!senderDigits.isNullOrBlank()) {
                AllowRuleEntity(
                    type = "SENDER",
                    value = senderDigits,
                    matchMode = "DIGITS",
                    enabled = true,
                    createdAt = System.currentTimeMillis()
                )
            } else {
                AllowRuleEntity(
                    type = "APP",
                    value = packageName,
                    matchMode = "EXACT",
                    enabled = true,
                    createdAt = System.currentTimeMillis()
                )
            }
            inboxDao.addAllowRule(rule)
            _uiEvents.emit(InboxUiEvent.ShowSnackbar("Sender added to Always allow"))
        }
    }

    fun copyCode(code: String) {
        viewModelScope.launch {
            _uiEvents.emit(InboxUiEvent.CopyToClipboard(code))
            _uiEvents.emit(InboxUiEvent.ShowSnackbar("Code copied: $code"))
        }
    }

    private fun mapToInboxRow(item: NotificationWithVerdict): InboxItem.Row {
        val parsedSignalClass = try {
            SignalClass.valueOf(item.signalClass)
        } catch (_: Exception) {
            SignalClass.ALERT
        }
        val parsedFirewallAction = item.firewallAction?.let {
            try {
                FirewallAction.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }
        val isSilenced = parsedFirewallAction == FirewallAction.SILENCE

        return InboxItem.Row(
            id = item.id,
            sbnKey = item.sbnKey,
            packageName = item.packageName,
            appLabel = item.appLabel,
            title = item.title,
            body = item.body,
            subText = item.subText,
            senderName = item.senderName,
            senderDigits = item.senderDigits,
            channelId = item.channelId,
            androidCategory = item.androidCategory,
            importance = item.importance,
            signalClass = parsedSignalClass,
            contentHash = item.contentHash,
            firstSeenAt = item.firstSeenAt,
            lastSeenAt = item.lastSeenAt,
            endedAt = item.endedAt,
            updateCount = item.updateCount,
            wasRateLimited = item.wasRateLimited,
            isSeen = item.isSeen,
            isStarred = item.isStarred,
            removalReason = item.removalReason,
            isSilenced = isSilenced,
            firewallAction = parsedFirewallAction,
            firewallRuleId = item.firewallRuleId,
            firewallRuleLabel = item.firewallRuleLabel
        )
    }

    private data class FilterParams(
        val query: String,
        val stateFilter: InboxStateFilter,
        val packageName: String?,
        val day: Int?,
        val showTransport: Boolean
    )
}
