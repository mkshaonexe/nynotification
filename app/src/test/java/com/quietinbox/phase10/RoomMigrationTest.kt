package com.quietinbox.phase10

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 10.11 — Room migration test.
 *
 * Verifies that:
 * 1. The Room schema JSON for version 1 is checked in and is valid.
 * 2. The schema contains every table specified in design.md §6.
 * 3. No `fallbackToDestructiveMigration` is present in QuietDatabase (audit A4 fix).
 */
class RoomMigrationTest {

    private val schemaFile: File? by lazy {
        // Try common schema export locations
        listOf(
            File("app/schemas/com.quietinbox.data.db.QuietDatabase/1.json"),
            File("schemas/com.quietinbox.data.db.QuietDatabase/1.json"),
        ).firstOrNull { it.exists() }
    }

    @Test
    fun schemaVersion1JsonExistsAndIsCheckedIn() {
        assertTrue(
            "Room schema for QuietDatabase version 1 must be checked in. " +
                "Run a debug build to generate it, then `git add app/schemas/`.",
            schemaFile != null && schemaFile!!.exists()
        )
    }

    @Test
    fun schemaContainsNotificationsTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'notifications' table (design.md §6).",
            schema.contains("\"notifications\"")
        )
    }

    @Test
    fun schemaContainsFirewallDecisionsTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'firewall_decisions' table (design.md §6).",
            schema.contains("firewall_decisions")
        )
    }

    @Test
    fun schemaContainsAllowRulesTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'allow_rules' table (design.md §6).",
            schema.contains("allow_rules")
        )
    }

    @Test
    fun schemaContainsMutedAppsTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'muted_apps' table (design.md §6).",
            schema.contains("muted_apps")
        )
    }

    @Test
    fun schemaContainsSchedulesTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'schedules' table (design.md §6).",
            schema.contains("\"schedules\"")
        )
    }

    @Test
    fun schemaContainsScheduleAppsTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'schedule_apps' join table (design.md §6).",
            schema.contains("schedule_apps")
        )
    }

    @Test
    fun schemaContainsDailyStatsTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'daily_stats' materialized table (design.md §6).",
            schema.contains("daily_stats")
        )
    }

    @Test
    fun schemaContainsAppCacheTable() {
        val schema = schemaFile?.readText() ?: return
        assertTrue(
            "Schema v1 must declare the 'app_cache' table (design.md §6).",
            schema.contains("app_cache")
        )
    }

    @Test
    fun databaseDoesNotUseFallbackToDestructiveMigration() {
        val dbFile = findFile("QuietDatabase.kt") ?: return
        val content = dbFile.readText()
        assertTrue(
            "QuietDatabase must NOT use fallbackToDestructiveMigration. " +
                "This is audit finding A4 — every schema change must have a real Migration object.",
            !content.contains("fallbackToDestructiveMigration")
        )
    }

    @Test
    fun databaseHasExportSchemaTrue() {
        val dbFile = findFile("QuietDatabase.kt") ?: return
        val content = dbFile.readText()
        assertTrue(
            "QuietDatabase must have exportSchema = true so migrations are traceable.",
            content.contains("exportSchema = true")
        )
    }

    private fun findFile(name: String): File? =
        File("app/src/main/java").walkTopDown().find { it.name == name }
            ?: File("src/main/java").walkTopDown().find { it.name == name }
}
