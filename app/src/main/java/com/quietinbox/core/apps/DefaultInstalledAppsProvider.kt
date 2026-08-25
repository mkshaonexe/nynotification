package com.quietinbox.core.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.quietinbox.data.db.dao.NotificationDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultInstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
) : InstalledAppsProvider {

    private val iconDir by lazy {
        File(context.filesDir, "appicons").apply { if (!exists()) mkdirs() }
    }

    override suspend fun all(includeSystemComponents: Boolean): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val counts = runCatching {
            notificationDao.getPackageCounts().associate { it.packageName to it.count }
        }.getOrDefault(emptyMap())

        val installedPackages = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }.getOrDefault(emptyList())

        val apps = installedPackages.map { appInfo ->
            val pkg = appInfo.packageName
            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg)
            val isLaunchable = pm.getLaunchIntentForPackage(pkg) != null
            val isPreinstalled = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val wasUpdated = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val notificationCount = counts[pkg] ?: 0

            InstalledApp(
                packageName = pkg,
                label = label,
                isLaunchable = isLaunchable,
                isPreinstalled = isPreinstalled,
                wasUpdatedSystemApp = wasUpdated,
                notificationCount = notificationCount
            )
        }

        val filtered = if (includeSystemComponents) {
            apps
        } else {
            apps.filter { it.isUserFacing }
        }

        filtered.sortedWith(
            compareByDescending<InstalledApp> { it.notificationCount }
                .thenBy { it.label.lowercase() }
        )
    }

    override suspend fun label(packageName: String): String = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    override fun iconFile(packageName: String): File {
        val file = File(iconDir, "$packageName.webp")
        if (!file.exists()) {
            saveAppIcon(packageName, file)
        }
        return file
    }

    private fun saveAppIcon(packageName: String, targetFile: File) {
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            FileOutputStream(targetFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
                }
            }
        } catch (_: Exception) {
            // If icon extraction fails, iconFile will be created on next demand
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
