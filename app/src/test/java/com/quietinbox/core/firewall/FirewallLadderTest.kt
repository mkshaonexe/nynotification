package com.quietinbox.core.firewall

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.RuleType
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.data.db.entity.SchedulePolicy
import com.quietinbox.data.prefs.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirewallLadderTest {

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
                leaveSystemAndMediaAlone = true,
                otpAlwaysBreaksThrough = true
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

    private fun createNotification(
        packageName: String = "com.example.chat",
        title: String? = "Hello",
        body: String? = "How are you?",
        subText: String? = null,
        senderName: String? = null,
        senderDigits: String? = null,
        category: String? = null,
        isOngoing: Boolean = false
    ): CapturedNotification {
        return CapturedNotification(
            sbnKey = "0|$packageName|1|tag|1000",
            packageName = packageName,
            appLabel = "Chat App",
            title = title,
            body = body,
            subText = subText,
            senderName = senderName,
            senderDigits = senderDigits,
            channelId = "chat_channel",
            androidCategory = category,
            importance = 3,
            postedAt = 1000L,
            isOngoing = isOngoing,
            isForegroundService = isOngoing,
            isGroupSummary = false,
            isClearable = !isOngoing,
            hasProgress = false,
            groupKey = null
        )
    }

    // --- INDIVIDUAL RULE TESTS (0 to 9) ---

    @Test
    fun `Rule 0 - package is our own app yields ALLOW`() = runTest {
        val notif = createNotification(packageName = context.packageName)
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_OUR_APP, verdict.ruleId)
    }

    @Test
    fun `Rule 1 - category alarm yields ALLOW`() = runTest {
        val notif = createNotification(category = "alarm")
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SAFETY_FLOOR, verdict.ruleId)
    }

    @Test
    fun `Rule 1 - category call yields ALLOW`() = runTest {
        val notif = createNotification(category = "call")
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SAFETY_FLOOR, verdict.ruleId)
    }

    @Test
    fun `Rule 2 - ongoing system or media controls yield ALLOW when enabled`() = runTest {
        settingsDataStore.setLeaveSystemAndMediaAlone(true)
        val notif = createNotification(packageName = "com.spotify.music", isOngoing = true)

        val verdictTransport = engine.evaluate(notif, SignalClass.TRANSPORT)
        assertEquals(FirewallAction.ALLOW, verdictTransport.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SYSTEM_MEDIA, verdictTransport.ruleId)

        val verdictService = engine.evaluate(notif, SignalClass.SERVICE)
        assertEquals(FirewallAction.ALLOW, verdictService.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SYSTEM_MEDIA, verdictService.ruleId)
    }

    @Test
    fun `Rule 3 - active pause yields ALLOW`() = runTest {
        clock.currentEpochMs = 100_000L
        settingsDataStore.setPausedUntilEpochMs(150_000L)

        val notif = createNotification()
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_PAUSED, verdict.ruleId)
    }

    @Test
    fun `Rule 3 - expired pause self-clears and falls through`() = runTest {
        clock.currentEpochMs = 200_000L
        settingsDataStore.setPausedUntilEpochMs(150_000L) // expired!
        settingsDataStore.setQuietModeEnabled(true)

        val notif = createNotification()
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        // Self-cleared and fell through to Quiet Mode (Rule 8)
        assertEquals(FirewallAction.SILENCE, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_QUIET_MODE, verdict.ruleId)
        assertNull(settingsDataStore.settings.first().pausedUntilEpochMs)
    }

    @Test
    fun `Rule 4 - always allow rule matches and yields ALLOW`() = runTest {
        ruleDao.enabledRules.add(
            AllowRuleEntity(id = 1, type = RuleType.APP, value = "com.example.chat", enabled = true)
        )
        val notif = createNotification(packageName = "com.example.chat")
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_ALWAYS_ALLOW, verdict.ruleId)
    }

    @Test
    fun `Rule 5 - OTP verification code yields ALLOW when enabled`() = runTest {
        settingsDataStore.setOtpAlwaysBreaksThrough(true)
        val notif = createNotification(
            title = "Bank OTP",
            body = "Your verification code is 849201"
        )
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_OTP, verdict.ruleId)
    }

    @Test
    fun `Rule 6 - active schedule OPEN yields ALLOW`() = runTest {
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(
                id = 1,
                name = "Work Hours",
                startMinute = 540,
                endMinute = 1080,
                daysMask = 127,
                policy = SchedulePolicy.OPEN,
                enabled = true,
                createdAt = 0L
            ),
            extraAllowedApps = emptySet()
        )
        val notif = createNotification()
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SCHEDULE, verdict.ruleId)
    }

    @Test
    fun `Rule 6 - active schedule QUIET yields SILENCE unless extra allowed`() = runTest {
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(
                id = 1,
                name = "Deep Focus",
                startMinute = 540,
                endMinute = 1080,
                daysMask = 127,
                policy = SchedulePolicy.QUIET,
                enabled = true,
                createdAt = 0L
            ),
            extraAllowedApps = setOf("com.slack")
        )

        val silencedNotif = createNotification(packageName = "com.instagram.android")
        val verdictSilenced = engine.evaluate(silencedNotif, SignalClass.ALERT)
        assertEquals(FirewallAction.SILENCE, verdictSilenced.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SCHEDULE, verdictSilenced.ruleId)

        val allowedNotif = createNotification(packageName = "com.slack")
        val verdictAllowed = engine.evaluate(allowedNotif, SignalClass.ALERT)
        assertEquals(FirewallAction.ALLOW, verdictAllowed.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SCHEDULE, verdictAllowed.ruleId)
    }

    @Test
    fun `Rule 7 - muted app yields SILENCE even when Quiet Mode is OFF`() = runTest {
        settingsDataStore.setQuietModeEnabled(false)
        val mutedPackage = "com.noisy.game"
        ruleDao.mutedApps.add(mutedPackage)

        val notif = createNotification(packageName = mutedPackage)
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.SILENCE, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_MUTED_APP, verdict.ruleId)
    }

    @Test
    fun `Rule 8 - quiet mode ON yields SILENCE`() = runTest {
        settingsDataStore.setQuietModeEnabled(true)
        val notif = createNotification()
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.SILENCE, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_QUIET_MODE, verdict.ruleId)
    }

    @Test
    fun `Rule 9 - default yields ALLOW when Quiet Mode is OFF and no rule matched`() = runTest {
        settingsDataStore.setQuietModeEnabled(false)
        val notif = createNotification()
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_DEFAULT, verdict.ruleId)
    }

    // --- PRECEDENCE TESTS (Rule X beats Rule Y) ---

    @Test
    fun `Precedence - Rule 0 (Our App) beats Rule 7 (Muted App) and Rule 8 (Quiet Mode)`() = runTest {
        ruleDao.mutedApps.add(context.packageName)
        settingsDataStore.setQuietModeEnabled(true)

        val notif = createNotification(packageName = context.packageName)
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_OUR_APP, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 1 (Alarm) beats Rule 6 (Quiet Schedule), Rule 7 (Muted App), and Rule 8 (Quiet Mode)`() = runTest {
        ruleDao.mutedApps.add("com.alarm.app")
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(1, "Quiet", 0, 1440, 127, SchedulePolicy.QUIET, true, 0L),
            extraAllowedApps = emptySet()
        )

        val notif = createNotification(packageName = "com.alarm.app", category = "alarm")
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SAFETY_FLOOR, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 2 (System Media) beats Rule 7 (Muted App) and Rule 8 (Quiet Mode)`() = runTest {
        ruleDao.mutedApps.add("com.spotify.music")
        settingsDataStore.setQuietModeEnabled(true)
        settingsDataStore.setLeaveSystemAndMediaAlone(true)

        val notif = createNotification(packageName = "com.spotify.music", isOngoing = true)
        val verdict = engine.evaluate(notif, SignalClass.TRANSPORT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SYSTEM_MEDIA, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 3 (Paused) beats Rule 7 (Muted App) and Rule 8 (Quiet Mode)`() = runTest {
        clock.currentEpochMs = 100_000L
        settingsDataStore.setPausedUntilEpochMs(200_000L)
        ruleDao.mutedApps.add("com.muted.pkg")
        settingsDataStore.setQuietModeEnabled(true)

        val notif = createNotification(packageName = "com.muted.pkg")
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_PAUSED, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 4 (Always Allow) beats Rule 6 (Quiet Schedule), Rule 7 (Muted App), and Rule 8 (Quiet Mode)`() = runTest {
        val pkg = "com.important.chat"
        ruleDao.enabledRules.add(AllowRuleEntity(1, RuleType.APP, pkg, enabled = true))
        ruleDao.mutedApps.add(pkg)
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(1, "Quiet", 0, 1440, 127, SchedulePolicy.QUIET, true, 0L),
            extraAllowedApps = emptySet()
        )

        val notif = createNotification(packageName = pkg)
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_ALWAYS_ALLOW, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 5 (OTP) beats Rule 6 (Quiet Schedule), Rule 7 (Muted App), and Rule 8 (Quiet Mode)`() = runTest {
        val pkg = "com.bank.sms"
        ruleDao.mutedApps.add(pkg)
        settingsDataStore.setQuietModeEnabled(true)
        settingsDataStore.setOtpAlwaysBreaksThrough(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(1, "Quiet", 0, 1440, 127, SchedulePolicy.QUIET, true, 0L),
            extraAllowedApps = emptySet()
        )

        val notif = createNotification(
            packageName = pkg,
            title = "Bank",
            body = "Your OTP is 492019 for transaction."
        )
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_OTP, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 6 (Schedule OPEN) beats Rule 7 (Muted App) and Rule 8 (Quiet Mode)`() = runTest {
        val pkg = "com.social.app"
        ruleDao.mutedApps.add(pkg)
        settingsDataStore.setQuietModeEnabled(true)
        scheduleEvaluator.activeSchedule = ActiveSchedule(
            schedule = ScheduleEntity(1, "Open Window", 0, 1440, 127, SchedulePolicy.OPEN, true, 0L),
            extraAllowedApps = emptySet()
        )

        val notif = createNotification(packageName = pkg)
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.ALLOW, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_SCHEDULE, verdict.ruleId)
    }

    @Test
    fun `Precedence - Rule 7 (Muted App) beats Rule 9 (Default Allow)`() = runTest {
        val pkg = "com.noisy.app"
        ruleDao.mutedApps.add(pkg)
        settingsDataStore.setQuietModeEnabled(false)

        val notif = createNotification(packageName = pkg)
        val verdict = engine.evaluate(notif, SignalClass.ALERT)

        assertEquals(FirewallAction.SILENCE, verdict.action)
        assertEquals(DefaultFirewallEngine.RULE_ID_MUTED_APP, verdict.ruleId)
    }
}
