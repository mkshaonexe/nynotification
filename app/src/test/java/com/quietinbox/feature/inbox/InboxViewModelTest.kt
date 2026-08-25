package com.quietinbox.feature.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.quietinbox.core.apps.InstalledApp
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.firewall.FirewallAction
import com.quietinbox.core.health.HealthSnapshot
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import com.quietinbox.core.model.SignalClass
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.NotificationEntity
import com.quietinbox.data.prefs.AppSettings
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.feature.inbox.data.InboxDao
import com.quietinbox.feature.inbox.data.NotificationWithVerdict
import com.quietinbox.feature.inbox.model.InboxStateFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for InboxViewModel testing filter and search matrix, multi-select, and actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeInboxDao
    private lateinit var fakeAppsProvider: FakeInstalledAppsProvider
    private lateinit var fakeSettingsDataStore: FakeSettingsDataStore
    private lateinit var fakeListenerHealth: FakeListenerHealth
    private lateinit var viewModel: InboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeInboxDao()
        fakeAppsProvider = FakeInstalledAppsProvider()
        fakeSettingsDataStore = FakeSettingsDataStore()
        fakeListenerHealth = FakeListenerHealth()

        seedNotifications()

        viewModel = InboxViewModel(
            inboxDao = fakeDao,
            installedAppsProvider = fakeAppsProvider,
            settingsDataStore = fakeSettingsDataStore,
            listenerHealth = fakeListenerHealth,
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun seedNotifications() {
        val now = 1756200000000L
        fakeDao.notifications.addAll(
            listOf(
                NotificationEntity(
                    id = 1,
                    sbnKey = "key_1",
                    packageName = "com.whatsapp",
                    appLabel = "WhatsApp",
                    title = "Alice",
                    body = "Hey there! Meeting at 3pm.",
                    subText = null,
                    senderName = "Alice",
                    senderDigits = "1709093872",
                    channelId = "messages",
                    androidCategory = "CATEGORY_MESSAGE",
                    importance = 4,
                    signalClass = SignalClass.ALERT,
                    contentHash = "hash1",
                    firstSeenAt = now,
                    lastSeenAt = now,
                    isSeen = false,
                    isStarred = false
                ),
                NotificationEntity(
                    id = 2,
                    sbnKey = "key_2",
                    packageName = "com.google.android.gm",
                    appLabel = "Gmail",
                    title = "Bank OTP Code",
                    body = "Your verification code is 492102",
                    subText = null,
                    senderName = "Security",
                    senderDigits = null,
                    channelId = "auth",
                    androidCategory = "CATEGORY_MESSAGE",
                    importance = 4,
                    signalClass = SignalClass.ALERT,
                    contentHash = "hash2",
                    firstSeenAt = now - 100000,
                    lastSeenAt = now - 100000,
                    isSeen = true,
                    isStarred = true
                ),
                NotificationEntity(
                    id = 3,
                    sbnKey = "key_3",
                    packageName = "com.android.systemui",
                    appLabel = "Android System",
                    title = "Hotspot active",
                    body = "1 device connected · 3.10 MB",
                    subText = null,
                    senderName = null,
                    senderDigits = null,
                    channelId = "system",
                    androidCategory = "CATEGORY_SERVICE",
                    importance = 2,
                    signalClass = SignalClass.ONGOING,
                    contentHash = "hash3",
                    firstSeenAt = now - 600000,
                    lastSeenAt = now - 1000,
                    endedAt = now - 1000,
                    updateCount = 600,
                    isSeen = false,
                    isStarred = false
                )
            )
        )
    }

    @Test
    fun testInitialState() = runTest(testDispatcher) {
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(InboxStateFilter.ALL, viewModel.stateFilter.value)
        assertNull(viewModel.selectedPackage.value)
        assertNull(viewModel.selectedDay.value)
        assertTrue(viewModel.selectedIds.value.isEmpty())
        assertNull(viewModel.detailNotification.value)
    }

    @Test
    fun testFilterStateChange() = runTest(testDispatcher) {
        viewModel.onStateFilterChanged(InboxStateFilter.UNSEEN)
        assertEquals(InboxStateFilter.UNSEEN, viewModel.stateFilter.value)

        viewModel.onStateFilterChanged(InboxStateFilter.STARRED)
        assertEquals(InboxStateFilter.STARRED, viewModel.stateFilter.value)

        viewModel.onPackageFilterChanged("com.whatsapp")
        assertEquals("com.whatsapp", viewModel.selectedPackage.value)

        viewModel.onDayFilterChanged(20260826)
        assertEquals(20260826, viewModel.selectedDay.value)

        viewModel.clearFilters()
        assertEquals(InboxStateFilter.ALL, viewModel.stateFilter.value)
        assertNull(viewModel.selectedPackage.value)
        assertNull(viewModel.selectedDay.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun testSearchQueryUpdate() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("Alice")
        assertEquals("Alice", viewModel.searchQuery.value)
    }

    @Test
    fun testToggleStar() = runTest(testDispatcher) {
        viewModel.toggleStar(id = 1, currentStarred = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeDao.isStarred(1))

        viewModel.toggleStar(id = 1, currentStarred = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(fakeDao.isStarred(1))
    }

    @Test
    fun testSetSeenAndMarkAllSeen() = runTest(testDispatcher) {
        assertFalse(fakeDao.isSeen(1))

        viewModel.setSeen(1, true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeDao.isSeen(1))

        viewModel.markAllSeen()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeDao.isSeen(1))
        assertTrue(fakeDao.isSeen(2))
        assertTrue(fakeDao.isSeen(3))
    }

    @Test
    fun testMultiSelectAndBulkActions() = runTest(testDispatcher) {
        viewModel.toggleSelect(1)
        viewModel.toggleSelect(2)
        assertEquals(setOf(1L, 2L), viewModel.selectedIds.value)

        viewModel.markSelectedSeen(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeDao.isSeen(1))
        assertTrue(fakeDao.isSeen(2))
        assertTrue(viewModel.selectedIds.value.isEmpty())

        viewModel.selectAll(listOf(1L, 2L, 3L))
        assertEquals(3, viewModel.selectedIds.value.size)

        viewModel.clearSelection()
        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun testDeleteNotificationAndUndo() = runTest(testDispatcher) {
        val row = fakeDao.getNotificationById(1)
        assertNotNull(row)

        val inboxRow = com.quietinbox.feature.inbox.model.InboxItem.Row(
            id = 1,
            sbnKey = "key_1",
            packageName = "com.whatsapp",
            appLabel = "WhatsApp",
            title = "Alice",
            body = "Hey there!",
            subText = null,
            senderName = "Alice",
            senderDigits = null,
            channelId = null,
            androidCategory = null,
            importance = 4,
            signalClass = SignalClass.ALERT,
            contentHash = "hash1",
            firstSeenAt = 1000L,
            lastSeenAt = 1000L,
            endedAt = null,
            updateCount = 1,
            wasRateLimited = false,
            isSeen = false,
            isStarred = false,
            removalReason = null,
            isSilenced = false,
            firewallAction = null,
            firewallRuleId = null,
            firewallRuleLabel = null
        )

        viewModel.deleteNotification(inboxRow)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeDao.getNotificationById(1))

        // Trigger undo
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(fakeDao.getNotificationById(1))
    }

    @Test
    fun testMuteAppAndAlwaysAllow() = runTest(testDispatcher) {
        viewModel.muteApp("com.whatsapp")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeDao.mutedApps.any { it.packageName == "com.whatsapp" })

        viewModel.alwaysAllowSender(senderName = "Alice", senderDigits = null, packageName = "com.whatsapp")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeDao.allowRules.any { it.value == "Alice" && it.type == "SENDER" })
    }
}

// -------------------------------------------------------------
// FAKES FOR TESTING
// -------------------------------------------------------------

class FakeInboxDao : InboxDao {
    val notifications = mutableListOf<NotificationEntity>()
    val mutedApps = mutableListOf<MutedAppEntity>()
    val allowRules = mutableListOf<AllowRuleEntity>()
    val firewallDecisions = mutableListOf<FirewallDecisionEntity>()

    fun isStarred(id: Long): Boolean = notifications.find { it.id == id }?.isStarred == true
    fun isSeen(id: Long): Boolean = notifications.find { it.id == id }?.isSeen == true

    override fun pagingSource(
        stateFilter: String,
        packageName: String?,
        startEpochMs: Long?,
        endEpochMs: Long?,
        includeTransport: Boolean,
        query: String?,
        digitsQuery: String?
    ): PagingSource<Int, NotificationWithVerdict> {
        return object : PagingSource<Int, NotificationWithVerdict>() {
            override fun getRefreshKey(state: PagingState<Int, NotificationWithVerdict>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationWithVerdict> {
                val filtered = notifications.filter { n ->
                    val matchState = when (stateFilter) {
                        "UNSEEN" -> !n.isSeen
                        "SEEN" -> n.isSeen
                        "STARRED" -> n.isStarred
                        else -> true
                    }
                    val matchPkg = packageName == null || n.packageName == packageName
                    val matchDay = startEpochMs == null || (n.firstSeenAt in startEpochMs..endEpochMs!!)
                    val matchTransport = includeTransport || n.signalClass != SignalClass.TRANSPORT
                    val matchQuery = query.isNullOrBlank() ||
                            n.title?.contains(query, ignoreCase = true) == true ||
                            n.body?.contains(query, ignoreCase = true) == true ||
                            (digitsQuery != null && n.senderDigits?.contains(digitsQuery) == true)
                    matchState && matchPkg && matchDay && matchTransport && matchQuery
                }.map { n ->
                    NotificationWithVerdict(
                        id = n.id,
                        sbnKey = n.sbnKey,
                        packageName = n.packageName,
                        appLabel = n.appLabel,
                        title = n.title,
                        body = n.body,
                        subText = n.subText,
                        senderName = n.senderName,
                        senderDigits = n.senderDigits,
                        channelId = n.channelId,
                        androidCategory = n.androidCategory,
                        importance = n.importance,
                        signalClass = n.signalClass.name,
                        contentHash = n.contentHash,
                        firstSeenAt = n.firstSeenAt,
                        lastSeenAt = n.lastSeenAt,
                        endedAt = n.endedAt,
                        updateCount = n.updateCount,
                        wasRateLimited = n.wasRateLimited,
                        isSeen = n.isSeen,
                        isStarred = n.isStarred,
                        removalReason = n.removalReason,
                        firewallAction = "ALLOW",
                        firewallRuleId = 0,
                        firewallRuleLabel = "Allowed"
                    )
                }
                return LoadResult.Page(data = filtered, prevKey = null, nextKey = null)
            }
        }
    }

    override suspend fun getNotificationById(id: Long): NotificationEntity? =
        notifications.find { it.id == id }

    override fun observeNotificationById(id: Long): Flow<NotificationEntity?> =
        flowOf(notifications.find { it.id == id })

    override suspend fun getLatestFirewallDecision(sbnKey: String): FirewallDecisionEntity? =
        firewallDecisions.find { it.sbnKey == sbnKey }

    override fun observeNotificationCount(): Flow<Int> =
        flowOf(notifications.size)

    override suspend fun getNotificationCount(): Int =
        notifications.size

    override suspend fun setSeen(id: Long, isSeen: Boolean) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isSeen = isSeen)
        }
    }

    override suspend fun setSeenByIds(ids: List<Long>, isSeen: Boolean) {
        ids.forEach { setSeen(it, isSeen) }
    }

    override suspend fun markAllSeen() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isSeen = true)
        }
    }

    override suspend fun setStarred(id: Long, isStarred: Boolean) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isStarred = isStarred)
        }
    }

    override suspend fun setStarredByIds(ids: List<Long>, isStarred: Boolean) {
        ids.forEach { setStarred(it, isStarred) }
    }

    override suspend fun deleteById(id: Long) {
        notifications.removeAll { it.id == id }
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        notifications.removeAll { it.id in ids }
    }

    override suspend fun insertNotifications(notifications: List<NotificationEntity>) {
        this.notifications.addAll(notifications)
    }

    override suspend fun insertNotification(notification: NotificationEntity): Long {
        notifications.add(notification)
        return notification.id
    }

    override suspend fun muteApp(entity: MutedAppEntity) {
        mutedApps.add(entity)
    }

    override suspend fun addAllowRule(rule: AllowRuleEntity) {
        allowRules.add(rule)
    }

    override fun observeDistinctApps(): Flow<List<String>> =
        flowOf(notifications.map { it.packageName }.distinct())
}

class FakeInstalledAppsProvider : InstalledAppsProvider {
    private val apps = listOf(
        InstalledApp(
            packageName = "com.whatsapp",
            label = "WhatsApp",
            isLaunchable = true,
            isPreinstalled = false,
            wasUpdatedSystemApp = false,
            notificationCount = 10
        ),
        InstalledApp(
            packageName = "com.google.android.gm",
            label = "Gmail",
            isLaunchable = true,
            isPreinstalled = true,
            wasUpdatedSystemApp = false,
            notificationCount = 5
        )
    )

    override suspend fun all(includeSystemComponents: Boolean): List<InstalledApp> = apps
    override suspend fun label(packageName: String): String =
        apps.find { it.packageName == packageName }?.label ?: packageName
    override fun iconFile(packageName: String): File = File("/dummy/$packageName.webp")
}

class FakeSettingsDataStore : SettingsDataStore {
    private val _settings = MutableStateFlow(AppSettings())
    override val settingsFlow: Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun setQuietModeEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(quietModeEnabled = enabled)
    }

    override suspend fun setPausedUntilEpochMs(epochMs: Long?) {
        _settings.value = _settings.value.copy(pausedUntilEpochMs = epochMs)
    }

    override suspend fun setLeaveSystemAndMediaAlone(leaveAlone: Boolean) {
        _settings.value = _settings.value.copy(leaveSystemAndMediaAlone = leaveAlone)
    }

    override suspend fun setOtpAlwaysBreaksThrough(breaksThrough: Boolean) {
        _settings.value = _settings.value.copy(otpAlwaysBreaksThrough = breaksThrough)
    }

    override suspend fun setRetentionDays(days: Int) {
        _settings.value = _settings.value.copy(retentionDays = days)
    }

    override suspend fun setShowTransportInInbox(show: Boolean) {
        _settings.value = _settings.value.copy(showTransportInInbox = show)
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(appLockEnabled = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _settings.value = _settings.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setThemeMode(mode: String) {
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(dynamicColorEnabled = enabled)
    }
}

class FakeListenerHealth : ListenerHealth {
    private val _snapshot = MutableStateFlow(
        HealthSnapshot(
            state = HealthState.CONNECTED,
            lastCaptureAt = System.currentTimeMillis(),
            totalCaptured = 100L,
            ingestErrors = 0L
        )
    )
    override val snapshot: kotlinx.coroutines.flow.StateFlow<HealthSnapshot> = _snapshot.asStateFlow()
    override fun refresh() {}
    override fun forceRebind() {}
}
