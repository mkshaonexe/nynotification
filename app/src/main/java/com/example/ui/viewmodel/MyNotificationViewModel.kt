package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MyNotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application)

    // Master Toggle State
    private val _masterBlockEnabled = MutableStateFlow(true)
    val masterBlockEnabled: StateFlow<Boolean> = _masterBlockEnabled.asStateFlow()

    // Navigation State
    private val _selectedTab = MutableStateFlow("dashboard")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    // Search and Filter States for Vault
    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

    private val _searchCategory = MutableStateFlow("All")
    val searchCategory: StateFlow<String> = _searchCategory.asStateFlow()

    // UI Trigger for Toast/Alert Dialogue Simulator Results
    val simulationResult = mutableStateOf<SimulationAlert?>(null)

    // ==========================================
    // EXPOSED DATABASE FLOWS
    // ==========================================

    val allBlockedApps: StateFlow<List<BlockedAppEntity>> = repository.allBlockedAppsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedNotifications: StateFlow<List<SavedNotificationEntity>> = repository.allSavedNotificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val otpNotifications: StateFlow<List<SavedNotificationEntity>> = repository.otpNotificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFocusModes: StateFlow<List<FocusModeEntity>> = repository.allFocusModesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeFocusMode: StateFlow<FocusModeEntity?> = repository.allFocusModesFlow
        .map { list -> list.firstOrNull { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSchedules: StateFlow<List<ScheduleEntity>> = repository.allSchedulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKeywords: StateFlow<List<KeywordRuleEntity>> = repository.allKeywordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<NotificationLogEntity>> = repository.allLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<NotificationLogEntity>> = repository.getRecentHistoryLogsFlow(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = repository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Saved Notifications based on Search & Category
    val filteredSavedNotifications: StateFlow<List<SavedNotificationEntity>> = combine(
        allSavedNotifications, _searchTerm, _searchCategory
    ) { notifications, query, category ->
        notifications.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.body.contains(query, ignoreCase = true) ||
                    item.appLabel.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // ACTIONS & BUSINESS LOGIC
    // ==========================================

    fun setMasterBlockEnabled(enabled: Boolean) {
        _masterBlockEnabled.value = enabled
    }

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
    }

    fun setSearchTerm(query: String) {
        _searchTerm.value = query
    }

    fun setSearchCategory(category: String) {
        _searchCategory.value = category
    }

    // Toggle App Blocking Rule
    fun toggleAppBlock(app: BlockedAppEntity) {
        viewModelScope.launch {
            repository.updateBlockedApp(app.copy(isBlocked = !app.isBlocked, updatedAt = System.currentTimeMillis()))
        }
    }

    // Set App Interruption Priority Level
    fun setAppPriority(app: BlockedAppEntity, priority: String) {
        viewModelScope.launch {
            repository.updateBlockedApp(app.copy(priority = priority, updatedAt = System.currentTimeMillis()))
        }
    }

    // Set App Rate limits (max allowed per hour)
    fun setAppRateLimit(app: BlockedAppEntity, maxPerHour: Int) {
        viewModelScope.launch {
            repository.updateBlockedApp(app.copy(maxPerHour = maxPerHour, updatedAt = System.currentTimeMillis()))
        }
    }

    // Delete single message from local Vault
    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    // Mark notification as read
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    // Toggle pin annotation
    fun toggleNotificationPin(item: SavedNotificationEntity) {
        viewModelScope.launch {
            repository.setPinned(item.id, !item.isPinned)
        }
    }

    // Clear saved alert storage database fully
    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
        }
    }

    // Clear Logs history completely
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Toggle active state of a Focus Mode
    fun toggleFocusMode(mode: FocusModeEntity) {
        viewModelScope.launch {
            val isNowActive = !mode.isActive
            if (isNowActive) {
                // Deactivate others, enable this one
                repository.activateFocusMode(mode.id)
                // Log focus session start
                repository.logFocusSession(
                    FocusSessionEntity(
                        focusModeId = mode.id,
                        focusModeName = mode.name,
                        startTime = System.currentTimeMillis(),
                        endTime = System.currentTimeMillis() + 60 * 60 * 1000, // mock 1 hour duration
                        notificationsBlocked = 0,
                        wasCompleted = false
                    )
                )
            } else {
                repository.deactivateActiveFocusMode()
            }
        }
    }

    // Create a new customized Focus Mode
    fun createFocusMode(name: String, icon: String, colorHex: String, allowedApps: List<String>, blockedApps: List<String>) {
        viewModelScope.launch {
            val entity = FocusModeEntity(
                name = name,
                iconName = icon,
                colorHex = colorHex,
                allowedPackages = allowedApps.joinToString(","),
                blockedPackages = blockedApps.joinToString(",")
            )
            repository.insertFocusMode(entity)
        }
    }

    fun deleteFocusMode(mode: FocusModeEntity) {
        viewModelScope.launch {
            repository.deleteFocusMode(mode)
        }
    }

    // Manage quiet schedules
    fun addSchedule(name: String, startH: Int, startM: Int, endH: Int, endM: Int, linkedFocusId: Long) {
        viewModelScope.launch {
            val entity = ScheduleEntity(
                name = name,
                startHour = startH,
                startMinute = startM,
                endHour = endH,
                endMinute = endM,
                focusModeId = linkedFocusId
            )
            repository.insertSchedule(entity)
        }
    }

    fun deleteSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    // Keyword managing rules
    fun addKeyword(word: String, useWildcard: Boolean = false) {
        viewModelScope.launch {
            repository.insertKeyword(KeywordRuleEntity(keyword = word, useWildcard = useWildcard))
        }
    }

    fun deleteKeyword(kw: KeywordRuleEntity) {
        viewModelScope.launch {
            repository.deleteKeyword(kw)
        }
    }

    // ==========================================
    // NOTIFICATION SIMULATOR OPERATIONS
    // ==========================================

    fun simulateNotification(pkg: String, label: String, title: String, body: String, channel: String) {
        viewModelScope.launch {
            val result = repository.processSimulatedNotification(
                packageName = pkg,
                appLabel = label,
                title = title,
                body = body,
                channelId = channel,
                isMasterBlockEnabled = _masterBlockEnabled.value
            )
            simulationResult.value = SimulationAlert(
                appLabel = label,
                packageName = pkg,
                title = title,
                body = body,
                category = result.category,
                isAllowed = result.isAllowed,
                reason = result.reason
            )
        }
    }

    fun clearSimulationResult() {
        simulationResult.value = null
    }
}

data class SimulationAlert(
    val appLabel: String,
    val packageName: String,
    val title: String,
    val body: String,
    val category: String,
    val isAllowed: Boolean,
    val reason: String
)
