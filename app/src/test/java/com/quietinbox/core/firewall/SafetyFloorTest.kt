package com.quietinbox.core.firewall

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.data.db.entity.SchedulePolicy
import com.quietinbox.data.prefs.AppSettings
import com.quietinbox.data.prefs.SettingsDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SafetyFloorTest {

    private lateinit var context: Context
    private lateinit var clock: FakeClock
    private lateinit var ruleDao: FakeRuleDao
    private lateinit var settingsDataStore: FakeSettingsDataStore
    private lateinit var scheduleEvaluator: FakeScheduleEvaluator
    private lateinit var ruleMatcher: RuleMatcher
    private lateinit var otpDetector: OtpDetector
    private lateinit var decisionLogger: FirewallDecisionLogger
    private lateinit var engine: DefaultFirewallEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        clock = FakeClock(100_000L)
        ruleDao = FakeRuleDao()
        settingsDataStore = FakeSettingsDataStore(
            AppSettings(
                quietModeEnabled = true,
                leaveSystemAndMediaAlone = false,
                otpAlwaysBreaksThrough = false
            )
        )
        scheduleEvaluator = FakeScheduleEvaluator()
        ruleMatcher = RuleMatcher()
        otpDetector = OtpDetector()
        decisionLogger = FirewallDecisionLogger(ruleDao, Dispatchers.Unconfined)

        engine = DefaultFirewallEngine(
            context = context,
            ruleDao = ruleDao,
            settingsDataStore = settingsDataStore,
            scheduleEvaluator = scheduleEvaluator,
            ruleMatcher = ruleMatcher,
            otpDetector = otpDetector,
            decisionLogger = decisionLogger,
            clock = clock
        )
    }

    private fun createAlarmOrCallNotification(
        packageName: String = "com.google.android.deskclock",
        category: String?,
        isOngoing: Boolean = false
    ): CapturedNotification {
        return CapturedNotification(
            sbnKey = "0|$packageName|1|tag|1000",
            packageName = packageName,
            appLabel = "Alarm App",
            title = "Wake Up",
            body = "Alarm ringing",
            subText = null,
            senderName = null,
            senderDigits = null,
            channelId = "alarm_channel",
            androidCategory = category,
            importance = 4,
            postedAt = 1000L,
            isOngoing = isOngoing,
            isForegroundService = isOngoing,
            isGroupSummary = false,
            isClearable = false,
            hasProgress = false,
            groupKey = null
        )
    }

    @Test
    fun `alarm notification survives Quiet Mode, Muted App, and Active Quiet Schedule`() = runTest {
        // Setup: App is muted, Quiet Mode is ON, Active Quiet Schedule is active
        val alarmPackage = "com.google.android.deskclock"
        ruleDao.mutedApps.add(alarmPackage)
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(
                id = 1,
                name = "Deep Sleep",
                startMinute = 0,
                endMinute = 1440,
                daysMask = 127,
                policy = SchedulePolicy.QUIET,
                enabled = true,
                createdAt = 0L
            ),
            extraAllowedApps = emptySet()
        )

        val alarmNotif = createAlarmOrCallNotification(
            packageName = alarmPackage,
            category = "alarm"
        )

        val verdict = engine.evaluate(alarmNotif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SAFETY_FLOOR, verdict.ruleId)
    }

    @Test
    fun `call notification survives Quiet Mode, Muted App, and Active Quiet Schedule`() = runTest {
        val phonePackage = "com.google.android.dialer"
        ruleDao.mutedApps.add(phonePackage)
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(
                id = 1,
                name = "Deep Sleep",
                startMinute = 0,
                endMinute = 1440,
                daysMask = 127,
                policy = SchedulePolicy.QUIET,
                enabled = true,
                createdAt = 0L
            ),
            extraAllowedApps = emptySet()
        )

        val callNotif = createAlarmOrCallNotification(
            packageName = phonePackage,
            category = "call"
        )

        val verdict = engine.evaluate(callNotif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SAFETY_FLOOR, verdict.ruleId)
    }

    @Test
    fun `ongoing telecom notification survives Quiet Mode and Active Quiet Schedule`() = runTest {
        val telecomPackage = "com.android.server.telecom"
        ruleDao.mutedApps.add(telecomPackage)
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(
                id = 1,
                name = "Focus",
                startMinute = 0,
                endMinute = 1440,
                daysMask = 127,
                policy = SchedulePolicy.QUIET,
                enabled = true,
                createdAt = 0L
            ),
            extraAllowedApps = emptySet()
        )

        val activeCallNotif = createAlarmOrCallNotification(
            packageName = telecomPackage,
            category = null,
            isOngoing = true
        )

        val verdict = engine.evaluate(activeCallNotif, SignalClass.ONGOING)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SAFETY_FLOOR, verdict.ruleId)
    }

    @Test
    fun `non ongoing telecom notification without alarm or call category is silenced under Quiet Mode`() = runTest {
        val telecomPackage = "com.android.server.telecom"
        settingsDataStore.setQuietModeEnabled(true)

        val missedCallNotif = createAlarmOrCallNotification(
            packageName = telecomPackage,
            category = null,
            isOngoing = false
        )

        val verdict = engine.evaluate(missedCallNotif, SignalClass.ALERT)

        assertEquals(FirewallAction.SILENCE, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_QUIET_MODE, verdict.ruleId)
    }
}

// --- TEST DOUBLES ---

class FakeClock(var currentEpochMs: Long = 100_000L) : Clock {
    override fun now(): Long = currentEpochMs
}

class FakeScheduleEvaluator(var activeSchedule: ActiveSchedule? = null) : ScheduleEvaluator {
    override suspend fun activeAt(nowEpochMs: Long): ActiveSchedule? = activeSchedule
}

class FakeRuleDao : RuleDao {
    val enabledRules = mutableListOf<AllowRuleEntity>()
    val mutedApps = mutableSetOf<String>()
    val loggedDecisions = mutableListOf<FirewallDecisionEntity>()

    override suspend fun getEnabledRulesSync(): List<AllowRuleEntity> = enabledRules

    override suspend fun isAppMuted(packageName: String): Boolean = mutedApps.contains(packageName)

    override suspend fun insertDecisions(decisions: List<FirewallDecisionEntity>) {
        loggedDecisions.addAll(decisions)
    }

    override suspend fun insertDecision(decision: FirewallDecisionEntity) {
        loggedDecisions.add(decision)
    }

    override fun observeEnabledRules(): Flow<List<AllowRuleEntity>> {
        return MutableStateFlow(enabledRules)
    }

    override fun observeMutedApps(): Flow<List<MutedAppEntity>> {
        return MutableStateFlow(mutedApps.map { MutedAppEntity(it, 0L) })
    }

    override suspend fun insertRule(rule: AllowRuleEntity): Long {
        enabledRules.add(rule)
        return rule.id
    }

    override suspend fun deleteRule(id: Long) {
        enabledRules.removeAll { it.id == id }
    }

    override suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
        val idx = enabledRules.indexOfFirst { it.id == id }
        if (idx >= 0) {
            enabledRules[idx] = enabledRules[idx].copy(enabled = enabled)
        }
    }

    override suspend fun muteApp(packageName: String, mutedAt: Long) {
        mutedApps.add(packageName)
    }

    override suspend fun unmuteApp(packageName: String) {
        mutedApps.remove(packageName)
    }
}

class FakeSettingsDataStore(initialSettings: AppSettings = AppSettings()) : SettingsDataStore {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: Flow<AppSettings> = _settings

    override suspend fun getSettings(): AppSettings = _settings.value

    override suspend fun setQuietModeEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(quietModeEnabled = enabled)
    }

    override suspend fun setPausedUntilEpochMs(epochMs: Long?) {
        _settings.value = _settings.value.copy(pausedUntilEpochMs = epochMs)
    }

    override suspend fun clearPause() {
        _settings.value = _settings.value.copy(pausedUntilEpochMs = null)
    }

    override suspend fun setLeaveSystemAndMediaAlone(enabled: Boolean) {
        _settings.value = _settings.value.copy(leaveSystemAndMediaAlone = enabled)
    }

    override suspend fun setOtpAlwaysBreaksThrough(enabled: Boolean) {
        _settings.value = _settings.value.copy(otpAlwaysBreaksThrough = enabled)
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
