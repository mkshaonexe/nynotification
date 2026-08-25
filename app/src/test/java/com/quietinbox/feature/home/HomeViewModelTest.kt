package com.quietinbox.feature.home

import com.quietinbox.core.health.HealthSnapshot
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.prefs.AppSettings
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.feature.home.components.StatsRange
import com.quietinbox.feature.home.data.AppNotificationCount
import com.quietinbox.feature.home.data.DailySilencedCount
import com.quietinbox.feature.home.data.HomeStatsDao
import com.quietinbox.feature.home.data.HourCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeDao: FakeHomeStatsDao
    private lateinit var fakeSettingsDataStore: FakeSettingsDataStore
    private lateinit var fakeListenerHealth: FakeListenerHealth
    private lateinit var fakeClock: FakeClock

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeDao = FakeHomeStatsDao()
        fakeSettingsDataStore = FakeSettingsDataStore()
        fakeListenerHealth = FakeListenerHealth()
        fakeClock = FakeClock(initialTime = 1756180000000L) // Fixed timestamp

        viewModel = HomeViewModel(
            homeStatsDao = fakeDao,
            settingsDataStore = fakeSettingsDataStore,
            listenerHealth = fakeListenerHealth,
            clock = fakeClock
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state collects from dao and datastore correctly`() = runTest(testDispatcher) {
        fakeDao.capturedCountFlow.value = 120
        fakeDao.silencedCountFlow.value = 95
        fakeDao.allowedCountFlow.value = 25
        fakeDao.distinctAppsFlow.value = 14
        fakeDao.busiestHourFlow.value = HourCount(hour = 14, count = 30)
        fakeDao.topAppsFlow.value = listOf(
            AppNotificationCount(packageName = "com.instagram.android", appLabel = "Instagram", count = 40)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(120, state.metrics.capturedCount)
        assertEquals(95, state.metrics.silencedCount)
        assertEquals(25, state.metrics.letThroughCount)
        assertEquals(14, state.metrics.appsCount)
        assertEquals("2 PM", state.metrics.busiestHourFormatted)
        assertEquals("Instagram", state.metrics.noisiestAppLabel)
    }

    @Test
    fun `toggleQuietMode updates settings datastore`() = runTest(testDispatcher) {
        viewModel.toggleQuietMode(true)
        advanceUntilIdle()

        assertTrue(fakeSettingsDataStore.currentSettings.quietModeEnabled)

        viewModel.toggleQuietMode(false)
        advanceUntilIdle()

        assertFalse(fakeSettingsDataStore.currentSettings.quietModeEnabled)
    }

    @Test
    fun `pauseFor updates pause timestamp in datastore`() = runTest(testDispatcher) {
        viewModel.pauseFor(15 * 60 * 1000L)
        advanceUntilIdle()

        assertEquals(fakeClock.now() + 15 * 60 * 1000L, fakeSettingsDataStore.currentSettings.pausedUntilEpochMs)
    }

    @Test
    fun `resumeNow clears pause and enables quiet mode`() = runTest(testDispatcher) {
        fakeSettingsDataStore.setPausedUntil(fakeClock.now() + 10000L)
        advanceUntilIdle()

        viewModel.resumeNow()
        advanceUntilIdle()

        assertEquals(0L, fakeSettingsDataStore.currentSettings.pausedUntilEpochMs)
        assertTrue(fakeSettingsDataStore.currentSettings.quietModeEnabled)
    }

    @Test
    fun `toggleMuteApp inserts into or deletes from muted apps in dao`() = runTest(testDispatcher) {
        // App is not muted yet -> mute it
        viewModel.toggleMuteApp("com.slack", isCurrentlyMuted = false)
        advanceUntilIdle()

        assertTrue(fakeDao.mutedPackagesList.contains("com.slack"))

        // App is muted -> unmute it
        viewModel.toggleMuteApp("com.slack", isCurrentlyMuted = true)
        advanceUntilIdle()

        assertFalse(fakeDao.mutedPackagesList.contains("com.slack"))
    }

    @Test
    fun `range selection updates state`() = runTest(testDispatcher) {
        viewModel.setRange(StatsRange.DAYS_7)
        advanceUntilIdle()

        assertEquals(StatsRange.DAYS_7, viewModel.uiState.value.selectedRange)
    }
}

// ---- Test Fakes ----

class FakeClock(private var time: Long) : Clock {
    override fun now(): Long = time
    fun setTime(newTime: Long) { time = newTime }
}

class FakeListenerHealth : ListenerHealth {
    val healthSnapshotFlow = MutableStateFlow(
        HealthSnapshot(
            state = HealthState.CONNECTED,
            lastCaptureAt = 1756180000000L,
            totalCaptured = 100L,
            ingestErrors = 0L
        )
    )
    override val snapshot: kotlinx.coroutines.flow.StateFlow<HealthSnapshot> = healthSnapshotFlow.asStateFlow()
    override fun refresh() {}
    override fun forceRebind() {}
}

class FakeSettingsDataStore : SettingsDataStore {
    var currentSettings = AppSettings(
        quietModeEnabled = false,
        pausedUntilEpochMs = 0L,
        leaveSystemAndMediaAlone = true,
        otpAlwaysBreaksThrough = true,
        retentionDays = 90,
        showTransportInInbox = false,
        appLockEnabled = false,
        onboardingCompleted = true,
        themeMode = "SYSTEM",
        dynamicColorEnabled = false
    )

    val settingsMutableFlow = MutableStateFlow(currentSettings)
    override val settingsFlow: Flow<AppSettings> = settingsMutableFlow.asStateFlow()

    override suspend fun setQuietModeEnabled(enabled: Boolean) {
        currentSettings = currentSettings.copy(quietModeEnabled = enabled)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setPausedUntil(pausedUntilEpochMs: Long) {
        currentSettings = currentSettings.copy(pausedUntilEpochMs = pausedUntilEpochMs)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setLeaveSystemAndMediaAlone(enabled: Boolean) {
        currentSettings = currentSettings.copy(leaveSystemAndMediaAlone = enabled)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setOtpAlwaysBreaksThrough(enabled: Boolean) {
        currentSettings = currentSettings.copy(otpAlwaysBreaksThrough = enabled)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setRetentionDays(days: Int) {
        currentSettings = currentSettings.copy(retentionDays = days)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setShowTransportInInbox(show: Boolean) {
        currentSettings = currentSettings.copy(showTransportInInbox = show)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        currentSettings = currentSettings.copy(appLockEnabled = enabled)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        currentSettings = currentSettings.copy(onboardingCompleted = completed)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setThemeMode(mode: String) {
        currentSettings = currentSettings.copy(themeMode = mode)
        settingsMutableFlow.value = currentSettings
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        currentSettings = currentSettings.copy(dynamicColorEnabled = enabled)
        settingsMutableFlow.value = currentSettings
    }
}

class FakeHomeStatsDao : HomeStatsDao {
    val capturedCountFlow = MutableStateFlow(0)
    val silencedCountFlow = MutableStateFlow(0)
    val allowedCountFlow = MutableStateFlow(0)
    val distinctAppsFlow = MutableStateFlow(0)
    val quietDaysFlow = MutableStateFlow(0)
    val dailyStatsFlow = MutableStateFlow<List<DailyStatEntity>>(emptyList())
    val busiestHourFlow = MutableStateFlow<HourCount?>(null)
    val topAppsFlow = MutableStateFlow<List<AppNotificationCount>>(emptyList())
    val mutedPackagesFlow = MutableStateFlow<List<String>>(emptyList())
    val dailySilencedFlow = MutableStateFlow<List<DailySilencedCount>>(emptyList())

    val mutedPackagesList = mutableSetOf<String>()

    override fun getCapturedCount(fromEpochMs: Long?): Flow<Int> = capturedCountFlow
    override fun getSilencedCount(fromEpochMs: Long?): Flow<Int> = silencedCountFlow
    override fun getAllowedCount(fromEpochMs: Long?): Flow<Int> = allowedCountFlow
    override fun getDistinctAppsCount(fromEpochMs: Long?): Flow<Int> = distinctAppsFlow
    override fun getQuietDaysCount(fromDayInt: Int?): Flow<Int> = quietDaysFlow
    override fun getAllDailyStats(): Flow<List<DailyStatEntity>> = dailyStatsFlow
    override fun getBusiestHour(fromEpochMs: Long?): Flow<HourCount?> = busiestHourFlow
    override fun getTopAppsByCount(fromEpochMs: Long?, limit: Int): Flow<List<AppNotificationCount>> = topAppsFlow
    override fun getMutedPackages(): Flow<List<String>> = mutedPackagesFlow

    override suspend fun muteApp(mutedApp: MutedAppEntity) {
        mutedPackagesList.add(mutedApp.packageName)
        mutedPackagesFlow.value = mutedPackagesList.toList()
    }

    override suspend fun unmuteApp(packageName: String) {
        mutedPackagesList.remove(packageName)
        mutedPackagesFlow.value = mutedPackagesList.toList()
    }

    override fun getDailySilencedFromStats(fromDayInt: Int?): Flow<List<DailySilencedCount>> = dailySilencedFlow
    override fun getDailySilencedFromFirewall(fromEpochMs: Long?): Flow<List<DailySilencedCount>> = dailySilencedFlow
}
