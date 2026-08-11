package com.netspeed.monitor.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.netspeed.monitor.MainActivity
import com.netspeed.monitor.R
import com.netspeed.monitor.data.NetworkSpeed
import com.netspeed.monitor.data.SpeedUnit
import com.netspeed.monitor.service.SpeedMonitorService
import com.netspeed.monitor.utils.SpeedFormatter

/**
 * Handles creation and dynamic updates of the persistent status bar notification,
 * featuring a dynamically rendered live speed number icon in the status bar (like battery %).
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Pre-allocated drawing objects to eliminate GC churn and jank across 1-second ticks
    private val iconBitmap: Bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    private val iconCanvas: Canvas = Canvas(iconBitmap)
    private val textBounds: Rect = Rect()
    private val iconPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    init {
        createNotificationChannel()
    }

    /**
     * Creates a low-importance NotificationChannel (no sound, no vibration, silent status bar presence).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates a dynamically-rendered status bar icon bitmap with the current speed number.
     * Reuses pre-allocated Canvas and Paint instances for optimal performance.
     */
    @Synchronized
    fun createSpeedIcon(speedText: String): IconCompat {
        // Clear previous frame to full transparency
        iconBitmap.eraseColor(Color.TRANSPARENT)

        // Select text size to fit within 96x96 status bar canvas
        val textSize = when {
            speedText.length <= 2 -> 58f
            speedText.length == 3 -> 50f
            speedText.length == 4 -> 42f
            else -> 36f
        }
        iconPaint.textSize = textSize

        // Center vertically and horizontally
        iconPaint.getTextBounds(speedText, 0, speedText.length, textBounds)
        val x = ICON_SIZE / 2f
        val y = (ICON_SIZE / 2f) - textBounds.exactCenterY()

        iconCanvas.drawText(speedText, x, y, iconPaint)

        return IconCompat.createWithBitmap(iconBitmap)
    }

    /**
     * Builds the persistent notification for the Foreground Service.
     */
    fun buildNotification(speed: NetworkSpeed, unit: SpeedUnit): Notification {
        val downloadStr = SpeedFormatter.formatDownload(speed.rxBytesPerSec, unit)
        val uploadStr = SpeedFormatter.formatUpload(speed.txBytesPerSec, unit)
        val combinedSpeed = "$downloadStr  $uploadStr"

        val totalDataStr = "Session: " + SpeedFormatter.formatDataUsage(speed.totalRxBytes + speed.totalTxBytes)

        // Use active throughput (max of rx/tx) so uploads also update the status bar number
        val activeSpeedBytes = maxOf(speed.rxBytesPerSec, speed.txBytesPerSec)
        val compactSpeed = SpeedFormatter.formatCompactSpeed(activeSpeedBytes, unit)

        val speedIcon = createSpeedIcon(compactSpeed)

        // PendingIntent to launch MainActivity on notification click
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intent to stop monitoring directly from the notification
        val stopIntent = Intent(context, SpeedMonitorService::class.java).apply {
            action = SpeedMonitorService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(speedIcon)
            .setContentTitle(combinedSpeed)
            .setContentText(totalDataStr)
            .setSubText(context.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Updates an existing notification with fresh speed metrics.
     */
    fun update(speed: NetworkSpeed, unit: SpeedUnit) {
        val notification = buildNotification(speed, unit)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_network_speed_monitor"
        const val NOTIFICATION_ID = 1001
        private const val ICON_SIZE = 96 // 96x96 px status bar icon size (xxhdpi standard)
    }
}
