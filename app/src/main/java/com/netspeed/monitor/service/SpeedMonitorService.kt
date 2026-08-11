package com.netspeed.monitor.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.netspeed.monitor.data.NetworkSpeed
import com.netspeed.monitor.data.PreferenceManager
import com.netspeed.monitor.engine.TrafficTracker
import com.netspeed.monitor.notification.NotificationHelper
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
 * Continuous Foreground Service that periodically samples network throughput via [TrafficStats],
 * applies moving average smoothing to eliminate flicker, updates the status bar icon, and publishes
 * metrics to the UI.
 *
 * Runs under android:foregroundServiceType="dataSync" to maintain reliable continuous execution
 * on Android 14+ (API 34) and Android 15 (API 35).
 */
class SpeedMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    // Dedicated Default dispatcher for predictable high-priority background polling
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var trafficTracker: TrafficTracker
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var preferenceManager: PreferenceManager

    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        trafficTracker = TrafficTracker()
        notificationHelper = NotificationHelper(this)
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
        val dualLine = preferenceManager.getDualLineIconEnabled()
        val initialNotification = notificationHelper.buildNotification(
            speed = NetworkSpeed.ZERO,
            unit = unit,
            dualLineIcon = dualLine
        )

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
            var tickIndex = 0L
            var lastLoopStartTime = 0L

            while (isActive) {
                tickIndex++
                val loopStartMs = SystemClock.elapsedRealtime()
                val intervalSinceLastLoopStartMs = if (lastLoopStartTime > 0L) loopStartMs - lastLoopStartTime else 0L

                // Detect large gaps (e.g. device was asleep / Doze for > 5s) and reset baselines
                // so we don't show a huge artificial spike after resuming
                if (lastLoopStartTime > 0L && intervalSinceLastLoopStartMs > MAX_LOOP_GAP_MS) {
                    Log.w(TAG, "[DOZE_GAP_DETECTED] Loop gap was ${intervalSinceLastLoopStartMs}ms. Resetting tracker baseline.")
                    trafficTracker.reset()
                }
                lastLoopStartTime = loopStartMs

                // Sync user configured smoothing window
                trafficTracker.windowSize = preferenceManager.getSmoothingWindowSize()

                val t0 = SystemClock.elapsedRealtime()
                val snapshot = trafficTracker.sampleSnapshot()
                val speed = snapshot.toNetworkSpeed()
                val sampleDurationMs = SystemClock.elapsedRealtime() - t0

                _currentSpeed.value = speed

                val t1 = SystemClock.elapsedRealtime()
                val speedUnit = preferenceManager.getSpeedUnit()
                val isDualLine = preferenceManager.getDualLineIconEnabled()
                notificationHelper.update(
                    speed = speed,
                    unit = speedUnit,
                    dualLineIcon = isDualLine
                )
                val notifyDurationMs = SystemClock.elapsedRealtime() - t1

                val totalWorkDurationMs = SystemClock.elapsedRealtime() - loopStartMs

                // Drift-compensated sleep to ensure precise 1000ms periodic intervals
                val delayTimeMs = (SAMPLE_INTERVAL_MS - totalWorkDurationMs).coerceAtLeast(10L)

                Log.d(
                    TAG,
                    "[LOOP_TICK #$tickIndex] intervalSinceLastStart=${intervalSinceLastLoopStartMs}ms | " +
                    "workDuration=${totalWorkDurationMs}ms (sample=${sampleDurationMs}ms, notify=${notifyDurationMs}ms) | " +
                    "scheduledDelay=${delayTimeMs}ms"
                )

                delay(delayTimeMs)
            }
        }
    }

    private fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
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
        private const val TAG = "SpeedDebug"
        const val ACTION_START_SERVICE = "com.netspeed.monitor.action.START"
        const val ACTION_STOP_SERVICE = "com.netspeed.monitor.action.STOP"
        private const val SAMPLE_INTERVAL_MS = 1000L
        private const val MAX_LOOP_GAP_MS = 5000L

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
