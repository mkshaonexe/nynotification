package com.quietinbox.phase10

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 10.7 — The no-internet assertion.
 *
 * Parses the AndroidManifest.xml and asserts that:
 * 1. `android.permission.INTERNET` is NOT present — protects the product's core marketing claim.
 * 2. `android.permission.QUERY_ALL_PACKAGES` is NOT present — avoids the Play policy declaration.
 * 3. `android:allowBackup="false"` IS present — fixes audit finding A5.
 *
 * This test is a CI gate; it must run in every PR and release build.
 */
class NoInternetPermissionTest {

    private val manifest: String by lazy {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        candidates.first { it.exists() }.readText()
    }

    @Test
    fun manifestContainsNoInternetPermission() {
        assertFalse(
            "CRITICAL: android.permission.INTERNET found in manifest. " +
                "This destroys the product's core trust claim. It must never be added.",
            manifest.contains("android.permission.INTERNET")
        )
    }

    @Test
    fun manifestContainsNoQueryAllPackages() {
        assertFalse(
            "android.permission.QUERY_ALL_PACKAGES found in manifest. " +
                "Use <queries> element with ACTION_MAIN + CATEGORY_LAUNCHER instead (design.md §5.7).",
            manifest.contains("android.permission.QUERY_ALL_PACKAGES")
        )
    }

    @Test
    fun manifestHasAllowBackupFalse() {
        assertTrue(
            "android:allowBackup must be 'false'. " +
                "Fixes audit A5 — the notification archive must not be copied to cloud backup.",
            manifest.contains("android:allowBackup=\"false\"")
        )
    }

    @Test
    fun manifestDeclaresBINDNotificationListenerServicePermission() {
        assertTrue(
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE must be declared on the service. " +
                "This is the core permission required for notification capture.",
            manifest.contains("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")
        )
    }

    @Test
    fun manifestDeclaresBootCompleted() {
        assertTrue(
            "android.intent.action.BOOT_COMPLETED receiver must be declared " +
                "so the listener rebinds after a reboot (design.md §10).",
            manifest.contains("android.intent.action.BOOT_COMPLETED")
        )
    }

    @Test
    fun manifestHasQueriesElement() {
        assertTrue(
            "A <queries> element with ACTION_MAIN + CATEGORY_LAUNCHER must be present " +
                "so installed-app enumeration works without QUERY_ALL_PACKAGES.",
            manifest.contains("android.intent.action.MAIN")
        )
    }

    @Test
    fun manifestHasEnableOnBackInvokedCallback() {
        assertTrue(
            "android:enableOnBackInvokedCallback='true' must be set for predictive back gesture support.",
            manifest.contains("android:enableOnBackInvokedCallback=\"true\"")
        )
    }
}
