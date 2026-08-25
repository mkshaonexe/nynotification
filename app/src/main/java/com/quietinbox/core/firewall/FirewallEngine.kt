package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass

enum class FirewallAction { ALLOW, SILENCE }

data class FirewallVerdict(
    val action: FirewallAction,
    val ruleId: Int,          // matches the rule numbers in design.md 5.3
    val ruleLabel: String,    // human readable, shown in the Inbox detail sheet
)

interface FirewallEngine {
    suspend fun evaluate(n: CapturedNotification, signalClass: SignalClass): FirewallVerdict
}
