package com.netspeed.monitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.netspeed.monitor.data.PreferenceManager
import com.netspeed.monitor.service.SpeedMonitorService

/**
 * BroadcastReceiver triggered when the device finishes booting or the app package is updated.
 * Automatically restores the continuous speed monitoring foreground service if enabled in preferences.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val preferenceManager = PreferenceManager.getInstance(context)

            if (preferenceManager.getMonitoringEnabled() && preferenceManager.getStartOnBoot()) {
                val serviceIntent = Intent(context, SpeedMonitorService::class.java).apply {
                    this.action = SpeedMonitorService.ACTION_START_SERVICE
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
