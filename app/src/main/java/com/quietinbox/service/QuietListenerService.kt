package com.quietinbox.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.health.DefaultListenerHealth
import com.quietinbox.core.time.Clock
import com.quietinbox.service.ingest.IngestPipeline
import com.quietinbox.service.ingest.NotificationParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuietListenerService : NotificationListenerService() {

    @Inject
    lateinit var ingestPipeline: IngestPipeline

    @Inject
    lateinit var installedAppsProvider: InstalledAppsProvider

    @Inject
    lateinit var listenerHealth: DefaultListenerHealth

    @Inject
    lateinit var clock: Clock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        listenerHealth.recordConnectionState(true)

        serviceScope.launch {
            runCatching {
                val active = activeNotifications ?: emptyArray()
                val parsed = active.map { sbn ->
                    val label = runCatching {
                        installedAppsProvider.label(sbn.packageName)
                    }.getOrDefault(sbn.packageName)
                    NotificationParser.parse(sbn = sbn, appLabel = label)
                }
                ingestPipeline.backfill(parsed)
            }.onFailure {
                listenerHealth.recordError()
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        listenerHealth.recordConnectionState(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        onNotificationPosted(sbn, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (sbn == null) return

        // Return immediately to avoid blocking system notification thread
        serviceScope.launch {
            runCatching {
                val label = runCatching {
                    installedAppsProvider.label(sbn.packageName)
                }.getOrDefault(sbn.packageName)

                val captured = NotificationParser.parse(
                    sbn = sbn,
                    rankingMap = rankingMap,
                    appLabel = label
                )

                val cancelKey = ingestPipeline.onPosted(captured)
                if (cancelKey != null && sbn.packageName != packageName) {
                    cancelNotification(cancelKey)
                }
            }.onFailure {
                listenerHealth.recordError()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        onNotificationRemoved(sbn, null, REASON_UNKNOWN)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        if (sbn == null) return

        serviceScope.launch {
            runCatching {
                ingestPipeline.onRemoved(sbn.key, reason)
            }.onFailure {
                listenerHealth.recordError()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val REASON_UNKNOWN = 0
    }
}
