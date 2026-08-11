package com.netspeed.monitor.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.netspeed.monitor.data.NetworkSpeed
import com.netspeed.monitor.data.PreferenceManager
import com.netspeed.monitor.engine.TrafficTracker
import com.netspeed.monitor.notification.NotificationHelper
import com.netspeed.monitor.overlay.FloatingOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Continuous Foreground Service that periodically samples network throughput using TrafficStats,
 * updates the persistent status bar notification, manages the optional floating overlay bubble,
 * and publishes real-time metrics to active UI collectors.
 */
class SpeedMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var trafficTracker: TrafficTracker
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: FloatingOverlayManager
    private lateinit var preferenceManager: PreferenceManager

    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        trafficTracker = TrafficTracker()
        notificationHelper = NotificationHelper(this)
        overlayManager = FloatingOverlayManager(this)
        preferenceManager = PreferenceManager.getInstance(this)

        _isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        startPollingLoop()

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val unit = preferenceManager.getSpeedUnit()
        val initialNotification = notificationHelper.buildNotification(NetworkSpeed.ZERO, unit)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, initialNotification)
        }
    }

    private fun startPollingLoop() {
        if (pollingJob?.isActive == true) return

        trafficTracker.reset()

        pollingJob = serviceScope.launch {
            while (isActive) {
                val speed = trafficTracker.sample()
                _currentSpeed.value = speed

                val displayMode = preferenceManager.getDisplayMode()
                val speedUnit = preferenceManager.getSpeedUnit()

                // Update notification if enabled in display mode
                if (displayMode.isNotificationActive) {
                    notificationHelper.update(speed, speedUnit)
                }

                // Update floating overlay if enabled in display mode
                if (displayMode.isOverlayActive) {
                    if (!overlayManager.isShowing && overlayManager.hasPermission()) {
                        overlayManager.show()
                    }
                    if (overlayManager.isShowing) {
                        overlayManager.updateSpeed(speed, speedUnit)
                    }
                } else {
                    if (overlayManager.isShowing) {
                        overlayManager.hide()
                    }
                }

                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    private fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        overlayManager.hide()
        _isServiceRunning.value = false
        preferenceManager.setMonitoringEnabled(false)
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "com.netspeed.monitor.action.START"
        const val ACTION_STOP_SERVICE = "com.netspeed.monitor.action.STOP"
        private const val SAMPLE_INTERVAL_MS = 1000L

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _currentSpeed = MutableStateFlow(NetworkSpeed.ZERO)
        val currentSpeed: StateFlow<NetworkSpeed> = _currentSpeed.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, SpeedMonitorService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SpeedMonitorService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
