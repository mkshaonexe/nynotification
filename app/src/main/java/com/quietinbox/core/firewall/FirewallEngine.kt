package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass

/**
 * Action taken by the firewall engine for a notification.
 */
enum class FirewallAction { ALLOW, SILENCE }

/**
 * Verdict resulting from evaluating a notification through the firewall ladder.
 *
 * @property action The delivery action to take ([FirewallAction.ALLOW] or [FirewallAction.SILENCE]).
 * @property ruleId The 0-based identifier matching the rule number in design.md §5.3.
 * @property ruleLabel Human-readable label explaining the decision, displayed in the Inbox detail sheet.
 */
data class FirewallVerdict(
    val action: FirewallAction,
    val ruleId: Int,
    val ruleLabel: String,
)

/**
 * Core interface responsible for evaluating incoming notifications against the delivery rules.
 */
interface FirewallEngine {
    /**
     * Evaluates a captured notification and its signal class through the 9-rule firewall ladder.
     *
     * @param n The captured notification to evaluate.
     * @param signalClass The signal class classified by the Signal Engine.
     * @return The firewall verdict ([FirewallVerdict]) indicating whether to allow or silence.
     */
    suspend fun evaluate(n: CapturedNotification, signalClass: SignalClass): FirewallVerdict
}
