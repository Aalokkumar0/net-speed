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
 * featuring real-time download and upload speed indicators dynamically rendered onto
 * a reusable Canvas bitmap.
 *
 * =========================================================================================
 * OEM STATUS BAR RENDERING CAVEATS & ARCHITECTURE:
 * 1. Stock Android & Google Pixel:
 *    Android since Lollipop (API 21+) treats notification small icons as strict alpha-masks.
 *    Any RGB color is discarded by the system status bar compositor; only alpha transparency
 *    is sampled and tinted (white in dark mode, dark gray in light mode).
 *    Setting [Paint.isAntiAlias = false] is critical: soft anti-aliased edges create translucent
 *    pixels that smear into blurry artifacts at ~24dp display scale. Binary opaque/transparent
 *    pixels ensure maximum legibility.
 *
 * 2. Samsung One UI (One UI 3.x - 8.x):
 *    One UI provides generous status bar icon bounds (~24-28dp) and handles monochrome bitmaps
 *    cleanly. Stacked dual-line monospace text ("↓1.2M" / "↑45K") renders with distinct vertical
 *    separation between download and upload rates.
 *
 * 3. Xiaomi (MIUI / HyperOS), OnePlus (OxygenOS), OPPO (ColorOS):
 *    These OEMs aggressively compress status bar icons. Using monospace bold typefaces and
 *    dynamic horizontal auto-scaling prevents character clipping across notches/punch-holes.
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
        // Anti-aliasing OFF: guarantees 100% opaque or 100% transparent pixels
        // Eliminates gray-fringing blur at status bar icon scale (~24dp)
        isAntiAlias = false
        color = Color.WHITE
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    init {
        createNotificationChannel()
    }

    /**
     * Creates a silent, low-importance NotificationChannel to ensure unobtrusive background presence.
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
     * Generates a status bar icon bitmap.
     * When [dualLine] is true, renders stacked two-line throughput ("↓ 1.2M" on top, "↑ 45K" on bottom).
     * When [dualLine] is false, renders a large single-line active throughput number.
     *
     * @param downBps Raw download throughput in bytes per second.
     * @param upBps Raw upload throughput in bytes per second.
     * @param unit Selected unit format (Bytes/s vs Bits/s).
     * @param dualLine True for stacked download/upload rows, false for single active rate.
     */
    @Synchronized
    fun generateSpeedIcon(
        downBps: Long,
        upBps: Long,
        unit: SpeedUnit = SpeedUnit.BYTES_PER_SEC,
        dualLine: Boolean = true
    ): IconCompat {
        // Clear canvas frame to full transparency
        iconBitmap.eraseColor(Color.TRANSPARENT)

        if (dualLine) {
            val downStr = "↓" + SpeedFormatter.formatCompactSpeed(downBps, unit)
            val upStr = "↑" + SpeedFormatter.formatCompactSpeed(upBps, unit)

            // Dynamic font scaling based on the longest string length
            val maxLen = maxOf(downStr.length, upStr.length)
            val textSize = when {
                maxLen <= 2 -> 40f  // e.g. "↓0", "↑0"
                maxLen == 3 -> 36f  // e.g. "↓5K", "↑1M"
                maxLen == 4 -> 32f  // e.g. "↓45K", "↑10M"
                else -> 28f         // e.g. "↓1.2M", "↓850K", "↓999M"
            }
            iconPaint.textSize = textSize

            // Calculate precise vertical centering for top row (Download)
            iconPaint.getTextBounds(downStr, 0, downStr.length, topBounds)
            val topX = ICON_SIZE / 2f
            val topY = (ICON_SIZE * 0.28f) - topBounds.exactCenterY()
            iconCanvas.drawText(downStr, topX, topY, iconPaint)

            // Calculate precise vertical centering for bottom row (Upload)
            iconPaint.getTextBounds(upStr, 0, upStr.length, botBounds)
            val botX = ICON_SIZE / 2f
            val botY = (ICON_SIZE * 0.74f) - botBounds.exactCenterY()
            iconCanvas.drawText(upStr, botX, botY, iconPaint)
        } else {
            // Single-line fallback: large single number for maximum distance legibility
            val activeBps = maxOf(downBps, upBps)
            val singleStr = SpeedFormatter.formatNumberOnlySpeed(activeBps, unit)

            val textSize = when (singleStr.length) {
                1 -> 72f    // "0" — single digit maximum size
                2 -> 58f    // "25" — two digits
                3 -> 46f    // "850", "1.2" — three chars
                else -> 38f // fallback
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
     * Builds the persistent notification for the Foreground Service.
     */
    fun buildNotification(
        speed: NetworkSpeed,
        unit: SpeedUnit,
        dualLineIcon: Boolean = true
    ): Notification {
        val speedIcon = generateSpeedIcon(
            downBps = speed.rxBytesPerSec,
            upBps = speed.txBytesPerSec,
            unit = unit,
            dualLine = dualLineIcon
        )

        val combinedSpeedTitle = SpeedFormatter.formatCombined(speed.rxBytesPerSec, speed.txBytesPerSec, unit)
        val sessionDataText = SpeedFormatter.formatDetailedDataUsage(speed.totalRxBytes, speed.totalTxBytes)

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

        // Action intent to stop monitoring directly from notification shade
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
     * Updates the persistent notification with the latest throughput measurements.
     */
    fun update(speed: NetworkSpeed, unit: SpeedUnit, dualLineIcon: Boolean = true) {
        val notification = buildNotification(speed, unit, dualLineIcon)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_network_speed_monitor"
        const val NOTIFICATION_ID = 1001
        private const val ICON_SIZE = 96 // 96x96 px (xxhdpi standard status bar canvas)
    }
}
