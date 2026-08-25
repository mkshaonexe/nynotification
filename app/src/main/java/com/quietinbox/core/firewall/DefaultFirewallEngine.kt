package com.quietinbox.core.firewall

import android.content.Context
import com.quietinbox.R
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.SchedulePolicy
import com.quietinbox.data.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [FirewallEngine].
 *
 * Implements the 9-rule evaluation ladder specified in design.md §5.3:
 * 0. Package is Quiet Inbox (internal) -> ALLOW
 * 1. Alarm / Call / Active telecom -> ALLOW (Safety floor, non-configurable)
 * 2. System & Media controls -> ALLOW (if leaveSystemAndMediaAlone is enabled)
 * 3. Quiet Mode Paused -> ALLOW (expired pause self-clears)
 * 4. Always-allow rules (App, Sender, Word) -> ALLOW
 * 5. OTP / Verification code -> ALLOW (if otpAlwaysBreaksThrough is enabled)
 * 6. Active Schedule -> ALLOW / SILENCE based on schedule policy
 * 7. Muted App -> SILENCE
 * 8. Quiet Mode ON -> SILENCE
 * 9. Otherwise -> ALLOW
 */
@Singleton
class DefaultFirewallEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleDao: RuleDao,
    private val settingsDataStore: SettingsDataStore,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val ruleMatcher: RuleMatcher,
    private val otpDetector: OtpDetector,
    private val decisionLogger: FirewallDecisionLogger,
    private val clock: Clock
) : FirewallEngine {

    companion object {
        const val RULE_ID_OUR_APP = 0
        const val RULE_ID_SAFETY_FLOOR = 1
        const val RULE_ID_SYSTEM_MEDIA = 2
        const val RULE_ID_PAUSED = 3
        const val RULE_ID_ALWAYS_ALLOW = 4
        const val RULE_ID_OTP = 5
        const val RULE_ID_SCHEDULE = 6
        const val RULE_ID_MUTED_APP = 7
        const val RULE_ID_QUIET_MODE = 8
        const val RULE_ID_DEFAULT = 9

        private const val CATEGORY_ALARM = "alarm"
        private const val CATEGORY_CALL = "call"
        private const val TELECOM_PACKAGE = "com.android.server.telecom"
    }

    override suspend fun evaluate(
        n: CapturedNotification,
        signalClass: SignalClass
    ): FirewallVerdict {
        val verdict = computeVerdict(n, signalClass)
        logDecision(n.sbnKey, verdict)
        return verdict
    }

    private suspend fun computeVerdict(
        n: CapturedNotification,
        signalClass: SignalClass
    ): FirewallVerdict {
        // Rule 0: Package is our own app -> ALLOW
        if (n.packageName == context.packageName) {
            return FirewallVerdict(
                action = FirewallAction.ALLOW,
                ruleId = RULE_ID_OUR_APP,
                ruleLabel = getStringSafely(R.string.rule_label_our_app, "Quiet Inbox notification")
            )
        }

        // Rule 1: Safety floor (CATEGORY_ALARM, CATEGORY_CALL, telecom ongoing) -> ALLOW
        if (isSafetyFloor(n)) {
            return FirewallVerdict(
                action = FirewallAction.ALLOW,
                ruleId = RULE_ID_SAFETY_FLOOR,
                ruleLabel = getStringSafely(R.string.rule_label_safety_floor, "Alarm or phone call")
            )
        }

        val settings = settingsDataStore.settings.first()
        val now = clock.now()

        // Rule 2: System & media pass-through -> ALLOW
        if (settings.leaveSystemAndMediaAlone && isSystemOrMedia(signalClass)) {
            return FirewallVerdict(
                action = FirewallAction.ALLOW,
                ruleId = RULE_ID_SYSTEM_MEDIA,
                ruleLabel = getStringSafely(R.string.rule_label_system_media, "System or media control")
            )
        }

        // Rule 3: Quiet Mode is paused -> ALLOW
        val pausedUntil = settings.pausedUntilEpochMs
        if (pausedUntil != null && pausedUntil > 0) {
            if (pausedUntil > now) {
                return FirewallVerdict(
                    action = FirewallAction.ALLOW,
                    ruleId = RULE_ID_PAUSED,
                    ruleLabel = getStringSafely(R.string.rule_label_paused, "Quiet Mode is paused")
                )
            } else {
                // Pause expired, self-clear
                settingsDataStore.clearPause()
            }
        }

        // Rule 4: Always-allow rules (App, Sender, Word) -> ALLOW
        val enabledRules = ruleDao.getEnabledRulesSync()
        val matchedRule = ruleMatcher.findMatchingRule(n, enabledRules)
        if (matchedRule != null) {
            return FirewallVerdict(
                action = FirewallAction.ALLOW,
                ruleId = RULE_ID_ALWAYS_ALLOW,
                ruleLabel = getStringSafely(R.string.rule_label_always_allow, "Always-allow rule")
            )
        }

        // Rule 5: OTP / Security code break-through -> ALLOW
        if (settings.otpAlwaysBreaksThrough && otpDetector.isOtp(n)) {
            return FirewallVerdict(
                action = FirewallAction.ALLOW,
                ruleId = RULE_ID_OTP,
                ruleLabel = getStringSafely(R.string.rule_label_otp, "Verification code")
            )
        }

        // Rule 6: Active Schedule -> ALLOW / SILENCE
        val activeSchedule = scheduleEvaluator.activeAt(now)
        if (activeSchedule != null) {
            val scheduleName = activeSchedule.schedule.name
            return if (activeSchedule.schedule.policy == SchedulePolicy.OPEN) {
                FirewallVerdict(
                    action = FirewallAction.ALLOW,
                    ruleId = RULE_ID_SCHEDULE,
                    ruleLabel = formatStringSafely(R.string.rule_label_schedule, "Schedule: $scheduleName", scheduleName)
                )
            } else {
                // SchedulePolicy.QUIET
                if (activeSchedule.extraAllowedApps.contains(n.packageName)) {
                    FirewallVerdict(
                        action = FirewallAction.ALLOW,
                        ruleId = RULE_ID_SCHEDULE,
                        ruleLabel = formatStringSafely(R.string.rule_label_schedule, "Schedule: $scheduleName", scheduleName)
                    )
                } else {
                    FirewallVerdict(
                        action = FirewallAction.SILENCE,
                        ruleId = RULE_ID_SCHEDULE,
                        ruleLabel = formatStringSafely(R.string.rule_label_schedule, "Schedule: $scheduleName", scheduleName)
                    )
                }
            }
        }

        // Rule 7: Muted apps -> SILENCE
        if (ruleDao.isAppMuted(n.packageName)) {
            return FirewallVerdict(
                action = FirewallAction.SILENCE,
                ruleId = RULE_ID_MUTED_APP,
                ruleLabel = getStringSafely(R.string.rule_label_muted_app, "Muted app")
            )
        }

        // Rule 8: Quiet Mode is ON -> SILENCE
        if (settings.quietModeEnabled) {
            return FirewallVerdict(
                action = FirewallAction.SILENCE,
                ruleId = RULE_ID_QUIET_MODE,
                ruleLabel = getStringSafely(R.string.rule_label_quiet_mode, "Quiet Mode is on")
            )
        }

        // Rule 9: Default -> ALLOW
        return FirewallVerdict(
            action = FirewallAction.ALLOW,
            ruleId = RULE_ID_DEFAULT,
            ruleLabel = getStringSafely(R.string.rule_label_default, "Default")
        )
    }

    private fun isSafetyFloor(n: CapturedNotification): Boolean {
        val category = n.androidCategory
        if (category != null) {
            if (category.equals(CATEGORY_ALARM, ignoreCase = true) ||
                category.equals(CATEGORY_CALL, ignoreCase = true)
            ) {
                return true
            }
        }
        if (n.packageName == TELECOM_PACKAGE && n.isOngoing) {
            return true
        }
        return false
    }

    private fun isSystemOrMedia(signalClass: SignalClass): Boolean {
        return signalClass == SignalClass.ONGOING ||
                signalClass == SignalClass.TRANSPORT ||
                signalClass == SignalClass.SERVICE
    }

    private fun logDecision(sbnKey: String, verdict: FirewallVerdict) {
        val decision = FirewallDecisionEntity(
            sbnKey = sbnKey,
            action = verdict.action,
            ruleId = verdict.ruleId,
            ruleLabel = verdict.ruleLabel,
            at = clock.now()
        )
        decisionLogger.log(decision)
    }

    private fun getStringSafely(resId: Int, fallback: String): String {
        return try {
            context.getString(resId)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun formatStringSafely(resId: Int, fallback: String, vararg args: Any): String {
        return try {
            context.getString(resId, *args)
        } catch (_: Exception) {
            fallback
        }
    }
}
