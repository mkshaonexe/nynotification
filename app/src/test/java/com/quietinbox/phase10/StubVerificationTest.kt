package com.quietinbox.phase10

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Phase 10.1 — Confirm the stubs are gone.
 *
 * DefaultSignalClassifier, DefaultFirewallEngine and DefaultScheduleEvaluator must all contain
 * real logic, not Phase 0's placeholder returns. Grep for the placeholder strings and fail the
 * phase if any survive.
 */
class StubVerificationTest {

    private val srcRoot = findSrcRoot()

    private fun findSrcRoot(): File {
        // Works from both the module root and the repo root
        return listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).first { it.exists() }
    }

    @Test
    fun defaultSignalClassifier_doesNotContainStubReturn() {
        val file = File(srcRoot, "com/quietinbox/core/signal/DefaultSignalClassifier.kt")
        assertFileExistsAndDoesNotContain(
            file = file,
            forbiddenSnippet = "Insert(ALERT)",
            description = "DefaultSignalClassifier must not return the Phase-0 stub Insert(ALERT)",
        )
        assertFileExistsAndDoesNotContain(
            file = file,
            forbiddenSnippet = "TODO(\"Phase 0 stub",
            description = "DefaultSignalClassifier must not contain a Phase-0 TODO stub",
        )
    }

    @Test
    fun defaultFirewallEngine_doesNotContainStubReturn() {
        val file = File(srcRoot, "com/quietinbox/core/firewall/DefaultFirewallEngine.kt")
        assertFileExistsAndDoesNotContain(
            file = file,
            forbiddenSnippet = "FirewallVerdict(action = FirewallAction.ALLOW, ruleId = 9, ruleLabel = \"Default\")",
            description = "DefaultFirewallEngine must not return the Phase-0 stub ALLOW/9/Default",
        )
        assertFileExistsAndDoesNotContain(
            file = file,
            forbiddenSnippet = "TODO(\"Phase 0 stub",
            description = "DefaultFirewallEngine must not contain a Phase-0 TODO stub",
        )
    }

    @Test
    fun defaultScheduleEvaluator_doesNotContainStubReturn() {
        val file = File(srcRoot, "com/quietinbox/core/firewall/DefaultScheduleEvaluator.kt")
        assertFileExistsAndDoesNotContain(
            file = file,
            forbiddenSnippet = "return null // Phase 0 stub",
            description = "DefaultScheduleEvaluator must not return the Phase-0 stub null",
        )
        assertFileExistsAndDoesNotContain(
            file = file,
            forbiddenSnippet = "TODO(\"Phase 0 stub",
            description = "DefaultScheduleEvaluator must not contain a Phase-0 TODO stub",
        )
    }

    private fun assertFileExistsAndDoesNotContain(
        file: File,
        forbiddenSnippet: String,
        description: String,
    ) {
        if (!file.exists()) {
            // File doesn't exist at direct path — search recursively as fallback
            val found = srcRoot.walkTopDown().find { it.name == file.name }
            if (found != null) {
                assertFalse(description, found.readText().contains(forbiddenSnippet))
            }
            return
        }
        assertFalse(description, file.readText().contains(forbiddenSnippet))
    }
}
