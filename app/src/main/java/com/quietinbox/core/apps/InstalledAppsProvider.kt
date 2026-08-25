package com.quietinbox.core.apps

import java.io.File

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isLaunchable: Boolean,
    val isPreinstalled: Boolean,
    val wasUpdatedSystemApp: Boolean,
    val notificationCount: Int,     // from our own DB; drives ordering
) {
    /** design.md 5.7 — Chrome must be a user app, the launcher must not. */
    val isUserFacing: Boolean get() = isLaunchable || wasUpdatedSystemApp || notificationCount > 0
    val isSystemComponent: Boolean get() = !isUserFacing && isPreinstalled
}

interface InstalledAppsProvider {
    suspend fun all(includeSystemComponents: Boolean): List<InstalledApp>
    suspend fun label(packageName: String): String
    fun iconFile(packageName: String): File
}
