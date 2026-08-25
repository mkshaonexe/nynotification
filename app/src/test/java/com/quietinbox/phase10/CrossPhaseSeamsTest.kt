package com.quietinbox.phase10

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 10.2 — Cross-phase seams verification.
 *
 * Confirms the key integration points between phases exist and are correctly wired:
 * - Heatmap → Inbox deep link (InboxFiltered route exists and is handled)
 * - Inbox "Mute this app" → Phase 6 muted_apps store
 * - Inbox "Always allow this sender" → Phase 3 rule matcher / allow_rules store
 * - Health banner → PermissionsHealth navigation route
 * - Firewall decision → Inbox detail sheet showing the rule that fired
 *
 * These are file-level integration checks (not runtime), asserting that the plumbing code
 * exists in each phase's output. Runtime integration is covered by endurance testing (10.8)
 * and device testing (10.9).
 */
class CrossPhaseSeamsTest {

    private val srcRoot = findSrcRoot()

    private fun findSrcRoot(): File = listOf(
        File("src/main/java"), File("app/src/main/java")
    ).first { it.exists() }

    // ── 10.2a: Heatmap → Inbox deep link ─────────────────────────────────────

    @Test
    fun inboxFilteredRouteExists() {
        val routesFile = srcRoot.walkTopDown().find { it.name == "Routes.kt" }
        assertNotNull("Routes.kt must exist in the navigation package", routesFile)
        assertTrue(
            "InboxFiltered route must be declared with optional day parameter for heatmap deep link",
            routesFile!!.readText().contains("InboxFiltered")
        )
    }

    @Test
    fun navHostHandlesInboxFilteredRoute() {
        val navHost = srcRoot.walkTopDown().find { it.name == "QuietNavHost.kt" }
        assertNotNull("QuietNavHost.kt must exist", navHost)
        assertTrue(
            "QuietNavHost must handle the InboxFiltered route so heatmap taps navigate to the filtered Inbox",
            navHost!!.readText().contains("InboxFiltered")
        )
    }

    @Test
    fun heatmapCellTriggersNavigation() {
        // The ActivityHeatmap component should emit a callback that the Home screen
        // uses to navigate to InboxFiltered
        val heatmapFile = srcRoot.walkTopDown().find { it.name == "ActivityHeatmap.kt" }
        assertNotNull("ActivityHeatmap.kt must exist in ui/components", heatmapFile)
        val content = heatmapFile!!.readText()
        assertTrue(
            "ActivityHeatmap must accept an onDayClick / onCellClick callback for the heatmap → Inbox deep link",
            content.contains("onDay") || content.contains("onCell") || content.contains("onClick")
        )
    }

    // ── 10.2b: Inbox "Mute this app" → Phase 6 muted_apps ───────────────────

    @Test
    fun inboxScreenHasMuteAppAction() {
        val inboxScreen = srcRoot.walkTopDown().find { it.name == "InboxScreen.kt" }
        assertNotNull("InboxScreen.kt must exist", inboxScreen)
        assertTrue(
            "InboxScreen must include a 'mute this app' action that writes to the muted_apps table",
            inboxScreen!!.readText().let { text ->
                text.contains("mute", ignoreCase = true) || text.contains("MutedApp")
            }
        )
    }

    @Test
    fun mutedAppsEntityExists() {
        val entity = srcRoot.walkTopDown().find { it.name == "MutedAppEntity.kt" }
        assertNotNull(
            "MutedAppEntity.kt must exist so the 'Mute this app' action has a Room table to write to",
            entity
        )
    }

    // ── 10.2c: Inbox "Always allow this sender" → Phase 3 rule matcher ───────

    @Test
    fun inboxDetailHasAlwaysAllowAction() {
        val inboxScreen = srcRoot.walkTopDown().find { it.name == "InboxScreen.kt" }
        assertNotNull("InboxScreen.kt must exist", inboxScreen)
        assertTrue(
            "InboxScreen detail sheet must include an 'Always allow this sender' action",
            inboxScreen!!.readText().let { text ->
                text.contains("allow", ignoreCase = true)
            }
        )
    }

    @Test
    fun allowRulesEntityExists() {
        val entity = srcRoot.walkTopDown().find { it.name == "AllowRuleEntity.kt" }
        assertNotNull(
            "AllowRuleEntity.kt must exist — 'Always allow' actions write to allow_rules",
            entity
        )
    }

    @Test
    fun firewallEngineConsumesAllowRules() {
        val engine = srcRoot.walkTopDown().find { it.name == "DefaultFirewallEngine.kt" }
        assertNotNull("DefaultFirewallEngine.kt must exist", engine)
        assertTrue(
            "FirewallEngine must reference RuleDao or RuleMatcher so Allow Rules actually break through",
            engine!!.readText().let { text ->
                text.contains("RuleDao") || text.contains("RuleMatcher") || text.contains("allowRule")
            }
        )
    }

    // ── 10.2d: Health banner → PermissionsHealth ─────────────────────────────

    @Test
    fun healthBannerHasNavigationCallback() {
        val healthBanner = srcRoot.walkTopDown().find { it.name == "HealthBanner.kt" }
        assertNotNull("HealthBanner.kt must exist in ui/components", healthBanner)
        val content = healthBanner!!.readText()
        assertTrue(
            "HealthBanner must accept an onFixClick or onNavigate callback that leads to PermissionsHealth",
            content.contains("onClick") || content.contains("onFix") || content.contains("onNavigate")
        )
    }

    @Test
    fun permissionsHealthRouteExists() {
        val routesFile = srcRoot.walkTopDown().find { it.name == "Routes.kt" }
        assertNotNull("Routes.kt must exist", routesFile)
        assertTrue(
            "PermissionsHealth route must be declared for the health banner fix action",
            routesFile!!.readText().contains("PermissionsHealth")
        )
    }

    // ── 10.2e: Firewall verdict surfaced in Inbox detail ─────────────────────

    @Test
    fun firewallDecisionEntityExists() {
        val entity = srcRoot.walkTopDown().find { it.name == "FirewallDecisionEntity.kt" }
        assertNotNull(
            "FirewallDecisionEntity.kt must exist so decisions are persisted and can be shown in the Inbox detail",
            entity
        )
    }

    @Test
    fun inboxDetailShowsFirewallVerdict() {
        val inboxScreen = srcRoot.walkTopDown().find { it.name == "InboxScreen.kt" }
        assertNotNull("InboxScreen.kt must exist", inboxScreen)
        assertTrue(
            "InboxScreen detail sheet must reference firewall verdict / ruleLabel for transparency",
            inboxScreen!!.readText().let { text ->
                text.contains("verdict", ignoreCase = true) ||
                    text.contains("ruleLabel", ignoreCase = true) ||
                    text.contains("firewall", ignoreCase = true) ||
                    text.contains("decision", ignoreCase = true)
            }
        )
    }
}
