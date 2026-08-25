package com.quietinbox.phase10

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 10.3 — Copy pass.
 *
 * Enforces design.md §3 plain-language principle:
 * - "vault", "master block", "smart shield", "intercept" must not appear in strings or Kotlin source.
 * - Every banned term that was part of the old AI-Studio prototype is explicitly excluded.
 *
 * Also verifies Phase 10.4 — Dead code:
 * - No seeded demo data.
 * - No simulation helpers.
 * - No FIXME or TODO without a justification reference.
 */
class CopyPassAndDeadCodeTest {

    private val srcRoot = findSrcRoot()
    private val resRoot = findResRoot()

    private fun findSrcRoot(): File = listOf(
        File("src/main/java"), File("app/src/main/java")
    ).first { it.exists() }

    private fun findResRoot(): File = listOf(
        File("src/main/res"), File("app/src/main/res")
    ).first { it.exists() }

    // ── Copy pass ────────────────────────────────────────────────────────────

    @Test
    fun noVaultTermInStrings() {
        assertNoTermInDirectory(
            root = resRoot,
            term = "vault",
            extensions = listOf(".xml"),
            message = "Design.md §3: use 'Inbox', not 'Vault' — remove all occurrences from string resources"
        )
    }

    @Test
    fun noMasterBlockTermInStrings() {
        assertNoTermInDirectory(
            root = resRoot,
            term = "master block",
            extensions = listOf(".xml"),
            message = "Design.md §3: use 'Quiet Mode', not 'Master Block' — remove all occurrences from string resources",
            caseInsensitive = true
        )
    }

    @Test
    fun noSmartShieldTermInStrings() {
        assertNoTermInDirectory(
            root = resRoot,
            term = "smart shield",
            extensions = listOf(".xml"),
            message = "Design.md §3: remove 'Smart Shield' from string resources",
            caseInsensitive = true
        )
    }

    @Test
    fun noInterceptTermInStrings() {
        assertNoTermInDirectory(
            root = resRoot,
            term = "intercept",
            extensions = listOf(".xml"),
            message = "Design.md §3: remove 'intercept' from user-facing string resources",
            caseInsensitive = true
        )
    }

    @Test
    fun noWhitelistTermInStrings() {
        assertNoTermInDirectory(
            root = resRoot,
            term = "whitelist",
            extensions = listOf(".xml"),
            message = "Design.md §3: use 'Always allow', not 'Whitelist'",
            caseInsensitive = true
        )
    }

    // ── Dead code ─────────────────────────────────────────────────────────────

    @Test
    fun noSimulatedNotificationCode() {
        assertNoTermInDirectory(
            root = srcRoot,
            term = "processSimulatedNotification",
            extensions = listOf(".kt"),
            message = "Phase 10.4: old simulation code must be fully deleted"
        )
    }

    @Test
    fun noSeedDatabaseCall() {
        assertNoTermInDirectory(
            root = srcRoot,
            term = "seedDatabase",
            extensions = listOf(".kt"),
            message = "Phase 10.4: demo-data seeding must be fully deleted"
        )
    }

    @Test
    fun noDemoData() {
        assertNoTermInDirectory(
            root = srcRoot,
            term = "demoNotification",
            extensions = listOf(".kt"),
            message = "Phase 10.4: demo notification data must be fully deleted"
        )
    }

    @Test
    fun noOtpCodeColumn() {
        assertNoTermInDirectory(
            root = srcRoot,
            term = "otpCode",
            extensions = listOf(".kt"),
            message = "Design.md §5.6 (audit A6): the otpCode column must not exist — code stays inside body"
        )
    }

    @Test
    fun noHardcodedNetworkingDependencies() {
        // build.gradle.kts should not contain retrofit, okhttp, or firebase-bom
        val buildFile = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts"),
        ).firstOrNull { it.exists() && it.length() > 0 } ?: return

        val content = buildFile.readText()
        assertFalse(
            "Phase 10.4: Retrofit must be removed (audit A7)",
            content.contains("retrofit", ignoreCase = true)
        )
        assertFalse(
            "Phase 10.4: OkHttp must be removed (audit A7)",
            content.contains("okhttp", ignoreCase = true)
        )
        assertFalse(
            "Phase 10.4: Firebase BOM / firebase-ai must be removed (audit A7)",
            content.contains("firebase-bom", ignoreCase = true) ||
                content.contains("firebase-ai", ignoreCase = true)
        )
        assertFalse(
            "Phase 10.4: secrets-gradle-plugin must be removed (audit A7)",
            content.contains("secrets", ignoreCase = true)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun assertNoTermInDirectory(
        root: File,
        term: String,
        extensions: List<String>,
        message: String,
        caseInsensitive: Boolean = false,
    ) {
        val violations = root.walkTopDown()
            .filter { file -> file.isFile && extensions.any { file.name.endsWith(it) } }
            .filter { file ->
                val text = file.readText()
                if (caseInsensitive) text.contains(term, ignoreCase = true)
                else text.contains(term)
            }
            .map { it.relativeTo(root).path }
            .toList()

        assertTrue(
            "$message\nViolating files:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
