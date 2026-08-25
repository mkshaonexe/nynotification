package com.quietinbox.core.firewall

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.dao.StatsDao
import com.quietinbox.data.db.dao.TopAppCapture
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.data.prefs.AppSettings
import com.quietinbox.data.prefs.SettingsDataStore
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
    private lateinit var statsDao: FakeStatsDao
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
        statsDao = FakeStatsDao()
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
        decisionLogger = FirewallDecisionLogger(statsDao, Dispatchers.Unconfined)

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
        val alarmPackage = "com.google.android.deskclock"
        ruleDao.mutedApps.add(alarmPackage)
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(
                id = 1L,
                name = "Deep Sleep",
                startMinute = 0,
                endMinute = 1440,
                daysMask = 127,
                policy = "QUIET",
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
                id = 1L,
                name = "Deep Sleep",
                startMinute = 0,
                endMinute = 1440,
                daysMask = 127,
                policy = "QUIET",
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
                id = 1L,
                name = "Focus",
                startMinute = 0,
                endMinute = 1440,
                daysMask = 127,
                policy = "QUIET",
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
    override fun elapsedRealtime(): Long = currentEpochMs
}

class FakeScheduleEvaluator(var activeSchedule: ActiveSchedule? = null) : ScheduleEvaluator {
    override suspend fun activeAt(nowEpochMs: Long): ActiveSchedule? = activeSchedule
}

class FakeRuleDao : RuleDao {
    val enabledRules = mutableListOf<AllowRuleEntity>()
    val mutedApps = mutableSetOf<String>()

    override suspend fun getEnabledRules(): List<AllowRuleEntity> = enabledRules.filter { it.enabled }

    override fun getAllRules(): Flow<List<AllowRuleEntity>> = MutableStateFlow(enabledRules)

    override fun getEnabledRulesFlow(): Flow<List<AllowRuleEntity>> =
        MutableStateFlow(enabledRules.filter { it.enabled })

    override suspend fun getRuleById(id: Long): AllowRuleEntity? = enabledRules.find { it.id == id }

    override suspend fun insertRule(rule: AllowRuleEntity): Long {
        enabledRules.add(rule)
        return rule.id
    }

    override suspend fun updateRule(rule: AllowRuleEntity) {
        val index = enabledRules.indexOfFirst { it.id == rule.id }
        if (index >= 0) enabledRules[index] = rule
    }

    override suspend fun deleteRule(rule: AllowRuleEntity) {
        enabledRules.removeAll { it.id == rule.id }
    }

    override suspend fun deleteRuleById(id: Long) {
        enabledRules.removeAll { it.id == id }
    }

    override fun getAllMutedApps(): Flow<List<MutedAppEntity>> =
        MutableStateFlow(mutedApps.map { MutedAppEntity(it, 0L) })

    override suspend fun getMutedPackageNames(): List<String> = mutedApps.toList()

    override suspend fun isAppMuted(packageName: String): Boolean = mutedApps.contains(packageName)

    override suspend fun muteApp(app: MutedAppEntity) {
        mutedApps.add(app.packageName)
    }

    override suspend fun unmuteApp(packageName: String) {
        mutedApps.remove(packageName)
    }
}

class FakeStatsDao : StatsDao {
    val loggedDecisions = mutableListOf<FirewallDecisionEntity>()

    override suspend fun getStats(day: Int): DailyStatEntity? = null
    override fun getRange(fromDay: Int, toDay: Int): Flow<List<DailyStatEntity>> = MutableStateFlow(emptyList())
    override suspend fun insertOrReplace(stat: DailyStatEntity) {}
    override suspend fun insertOrReplaceAll(stats: List<DailyStatEntity>) {}

    override suspend fun logDecision(decision: FirewallDecisionEntity): Long {
        loggedDecisions.add(decision)
        return decision.id
    }

    override suspend fun logDecisions(decisions: List<FirewallDecisionEntity>) {
        loggedDecisions.addAll(decisions)
    }

    override suspend fun getSilencedCountSince(sinceEpochMs: Long): Int =
        loggedDecisions.count { it.action == "SILENCE" && it.at >= sinceEpochMs }

    override suspend fun getAllowedCountSince(sinceEpochMs: Long): Int =
        loggedDecisions.count { it.action == "ALLOW" && it.at >= sinceEpochMs }

    override suspend fun getTopNotifyingApps(sinceEpochMs: Long, limit: Int): List<TopAppCapture> = emptyList()
}

class FakeSettingsDataStore(initialSettings: AppSettings = AppSettings()) : SettingsDataStore(
    ApplicationProvider.getApplicationContext()
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settingsFlow: Flow<AppSettings> = _settings

    suspend fun setQuietMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(quietModeEnabled = enabled)
    }

    suspend fun setPausedUntil(epochMs: Long) {
        _settings.value = _settings.value.copy(pausedUntilEpochMs = epochMs)
    }
}
