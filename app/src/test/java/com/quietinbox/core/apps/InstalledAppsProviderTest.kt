package com.quietinbox.core.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppsProviderTest {

    @Test
    fun chromeStyleLaunchablePreinstalledAppIsUserFacing() {
        val app = InstalledApp(
            packageName = "com.android.chrome",
            label = "Chrome",
            isLaunchable = true,
            isPreinstalled = true,
            wasUpdatedSystemApp = false,
            notificationCount = 0
        )

        assertTrue(app.isUserFacing)
        assertFalse(app.isSystemComponent)
    }

    @Test
    fun launcherStyleNonLaunchablePreinstalledAppIsSystemComponent() {
        val app = InstalledApp(
            packageName = "com.android.systemui",
            label = "System UI",
            isLaunchable = false,
            isPreinstalled = true,
            wasUpdatedSystemApp = false,
            notificationCount = 0
        )

        assertFalse(app.isUserFacing)
        assertTrue(app.isSystemComponent)
    }

    @Test
    fun unlaunchableAppWithNotificationCountIsUserFacing() {
        val app = InstalledApp(
            packageName = "com.example.carrier.service",
            label = "Carrier Service",
            isLaunchable = false,
            isPreinstalled = true,
            wasUpdatedSystemApp = false,
            notificationCount = 5
        )

        assertTrue(app.isUserFacing)
        assertFalse(app.isSystemComponent)
    }

    @Test
    fun updatedSystemAppIsUserFacing() {
        val app = InstalledApp(
            packageName = "com.google.android.youtube",
            label = "YouTube",
            isLaunchable = false,
            isPreinstalled = true,
            wasUpdatedSystemApp = true,
            notificationCount = 0
        )

        assertTrue(app.isUserFacing)
        assertFalse(app.isSystemComponent)
    }
}
