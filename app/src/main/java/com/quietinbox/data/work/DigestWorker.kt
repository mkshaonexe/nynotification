package com.quietinbox.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quietinbox.R
import com.quietinbox.core.time.Clock
import com.quietinbox.feature.settings.data.SettingsDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that delivers an optional daily focus summary notification.
 * Posts on its own dedicated channel and requires POST_NOTIFICATIONS permission on Android 13+.
 */
@HiltWorker
class DigestWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsDao: SettingsDao,
    private val clock: Clock
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "com.quietinbox.data.work.DigestWorker"
        const val CHANNEL_ID = "digest_channel"
        const val NOTIFICATION_ID = 9001
        const val MS_PER_DAY = 24L * 60 * 60 * 1000
        const val PREFS_NAME = "quiet_inbox_digest_prefs"
        const val KEY_DIGEST_ENABLED = "digest_enabled"
    }

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDigestEnabled = prefs.getBoolean(KEY_DIGEST_ENABLED, false)
        if (!isDigestEnabled) {
            return Result.success()
        }

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return Result.success()
            }
        }

        val nowMs = clock.now()
        val sinceMs = nowMs - MS_PER_DAY

        val silencedCount = settingsDao.getSilencedCountInRange(sinceMs, nowMs)
        val allowedCount = settingsDao.getAllowedCountInRange(sinceMs, nowMs)

        // Only notify if there was activity
        if (silencedCount > 0 || allowedCount > 0) {
            createNotificationChannel()

            val title = appContext.getString(R.string.digest_notification_title)
            val body = appContext.getString(
                R.string.digest_notification_body,
                silencedCount,
                allowedCount
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            runCatching {
                NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = appContext.getString(R.string.digest_channel_name)
            val description = appContext.getString(R.string.digest_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
