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
 * Handles creation and dynamic updates of the persistent notification and status bar icon.
 *
 * =========================================================================================
 * ANDROID FOREGROUND SERVICE NOTIFICATION REQUIREMENT:
 * Android OS (Android 8.0+ / API 26 through Android 15 / API 35) strictly mandates that any
 * Foreground Service (especially android:foregroundServiceType="dataSync") MUST be bound to
 * an active [Notification] via Service.startForeground().
 *
 * An app CANNOT simply remove or cancel the notification while the service is alive, or the OS
 * will immediately throw a ForegroundServiceDidNotStartInTimeException or kill the process.
 *
 * When the user turns OFF the "Show notification in drawer" toggle, we do NOT stop or destroy
 * the notification. Instead, we transition to [buildMinimalNotification]:
 * - Uses IMPORTANCE_MIN channel (completely silent, minimized/collapsed at the bottom of the shade)
 * - PRIORITY_MIN
 * - Removes all body text, session usage metrics, and action buttons
 * - CRITICAL: Still sets the dynamic [setSmallIcon] bitmap so the live speed indicator in the
 *   top status bar continues updating seamlessly every second without interruption.
 * =========================================================================================
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Pre-allocated drawing objects to eliminate GC allocations and frame drops on 1-second ticks
    private val iconBitmap: Bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    private val iconCanvas: Canvas = Canvas(iconBitmap)
    private val topBounds: Rect = Rect()
    private val botBounds: Rect = Rect()

    private val iconPaint: Paint = Paint().apply {
        isAntiAlias = false
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates notification channels for both full (low importance) and minimal (min importance) modes.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Full notification channel: Low importance (silent, no vibration, visible in shade)
            val fullChannel = NotificationChannel(
                CHANNEL_ID_FULL,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(fullChannel)

            // Minimal notification channel: Min importance (silent, collapsed/minimized in shade)
            val minChannel = NotificationChannel(
                CHANNEL_ID_MINIMAL,
                context.getString(R.string.notification_channel_name) + " (Silent / Minimized)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Minimized background channel for status-bar-only mode."
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(minChannel)
        }
    }

    /**
     * Generates a status bar icon bitmap.
     * When [dualLine] is true, renders stacked two-line throughput ("↓ 1.2M" on top, "↑ 45K" on bottom).
     * When [dualLine] is false, renders a large single-line active throughput number.
     */
    @Synchronized
    fun generateSpeedIcon(
        downBps: Long,
        upBps: Long,
        unit: SpeedUnit = SpeedUnit.BYTES_PER_SEC,
        dualLine: Boolean = true
    ): IconCompat {
        iconBitmap.eraseColor(Color.TRANSPARENT)

        if (dualLine) {
            val downStr = "↓" + SpeedFormatter.formatCompactSpeed(downBps, unit)
            val upStr = "↑" + SpeedFormatter.formatCompactSpeed(upBps, unit)

            val maxLen = maxOf(downStr.length, upStr.length)
            val textSize = when {
                maxLen <= 2 -> 48f
                maxLen == 3 -> 44f
                maxLen == 4 -> 40f
                else -> 36f
            }
            iconPaint.textSize = textSize

            iconPaint.getTextBounds(downStr, 0, downStr.length, topBounds)
            val topX = ICON_SIZE / 2f
            val topY = (ICON_SIZE * 0.28f) - topBounds.exactCenterY()
            iconCanvas.drawText(downStr, topX, topY, iconPaint)

            iconPaint.getTextBounds(upStr, 0, upStr.length, botBounds)
            val botX = ICON_SIZE / 2f
            val botY = (ICON_SIZE * 0.74f) - botBounds.exactCenterY()
            iconCanvas.drawText(upStr, botX, botY, iconPaint)
        } else {
            val activeBps = maxOf(downBps, upBps)
            val singleStr = SpeedFormatter.formatNumberOnlySpeed(activeBps, unit)

            val textSize = when (singleStr.length) {
                1 -> 86f
                2 -> 76f
                3 -> 62f
                else -> 50f
            }
            iconPaint.textSize = textSize

            iconPaint.getTextBounds(singleStr, 0, singleStr.length, topBounds)
            val x = ICON_SIZE / 2f
            val y = (ICON_SIZE / 2f) - topBounds.exactCenterY()
            iconCanvas.drawText(singleStr, x, y, iconPaint)
        }

        return IconCompat.createWithBitmap(iconBitmap)
    }

    /**
     * Resolves the small icon to display (either dynamic speed bitmap or static icon).
     */
    private fun resolveSmallIcon(
        speed: NetworkSpeed,
        unit: SpeedUnit,
        showSpeedIcon: Boolean,
        dualLineIcon: Boolean
    ): IconCompat {
        return if (showSpeedIcon) {
            generateSpeedIcon(
                downBps = speed.rxBytesPerSec,
                upBps = speed.txBytesPerSec,
                unit = unit,
                dualLine = dualLineIcon
            )
        } else {
            IconCompat.createWithResource(context, R.drawable.ic_speed_notification)
        }
    }

    /**
     * Builds the full detailed notification card (shows live throughput, session data, and Stop button).
     */
    fun buildFullNotification(
        speed: NetworkSpeed,
        unit: SpeedUnit,
        showSpeedIcon: Boolean = true,
        dualLineIcon: Boolean = true
    ): Notification {
        val speedIcon = resolveSmallIcon(speed, unit, showSpeedIcon, dualLineIcon)
        val combinedSpeedTitle = SpeedFormatter.formatCombined(speed.rxBytesPerSec, speed.txBytesPerSec, unit)
        val sessionDataText = SpeedFormatter.formatDetailedDataUsage(speed.totalRxBytes, speed.totalTxBytes)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, SpeedMonitorService::class.java).apply {
            action = SpeedMonitorService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_FULL)
            .setSmallIcon(speedIcon)
            .setContentTitle(combinedSpeedTitle)
            .setContentText(sessionDataText)
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
     * Builds a minimal, silent notification for status-bar-only mode.
     * Keeps the status bar icon active while collapsing/minimizing the drawer card.
     */
    fun buildMinimalNotification(
        speed: NetworkSpeed,
        unit: SpeedUnit,
        showSpeedIcon: Boolean = true,
        dualLineIcon: Boolean = true
    ): Notification {
        val speedIcon = resolveSmallIcon(speed, unit, showSpeedIcon, dualLineIcon)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_MINIMAL)
            .setSmallIcon(speedIcon)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(null)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Builds the appropriate notification based on user's [notificationVisible] preference.
     */
    fun buildNotification(
        speed: NetworkSpeed,
        unit: SpeedUnit,
        showSpeedIcon: Boolean = true,
        dualLineIcon: Boolean = true,
        notificationVisible: Boolean = true
    ): Notification {
        return if (notificationVisible) {
            buildFullNotification(speed, unit, showSpeedIcon, dualLineIcon)
        } else {
            buildMinimalNotification(speed, unit, showSpeedIcon, dualLineIcon)
        }
    }

    /**
     * Updates the active persistent notification.
     */
    fun update(
        speed: NetworkSpeed,
        unit: SpeedUnit,
        showSpeedIcon: Boolean = true,
        dualLineIcon: Boolean = true,
        notificationVisible: Boolean = true
    ) {
        val notification = buildNotification(
            speed = speed,
            unit = unit,
            showSpeedIcon = showSpeedIcon,
            dualLineIcon = dualLineIcon,
            notificationVisible = notificationVisible
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID_FULL = "channel_network_speed_monitor"
        const val CHANNEL_ID_MINIMAL = "channel_network_speed_monitor_min"
        const val NOTIFICATION_ID = 1001
        private const val ICON_SIZE = 96
    }
}
