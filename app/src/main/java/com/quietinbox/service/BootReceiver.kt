package com.quietinbox.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import com.quietinbox.core.health.ListenerHealth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var listenerHealth: ListenerHealth

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            runCatching {
                val component = ComponentName(context, QuietListenerService::class.java)
                NotificationListenerService.requestRebind(component)
                listenerHealth.refresh()
            }
        }
    }
}
