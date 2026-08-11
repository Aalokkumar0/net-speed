package com.netspeed.monitor.utils

import com.netspeed.monitor.data.SpeedUnit
import java.util.Locale

/**
 * Standardized network throughput formatter matching Android system status bar monitors
 * and telecom standards.
 */
object SpeedFormatter {

    private const val KB_BYTES = 1024.0
    private const val MB_BYTES = KB_BYTES * 1024.0
    private const val GB_BYTES = MB_BYTES * 1024.0

    private const val KB_BITS = 1000.0
    private const val MB_BITS = KB_BITS * 1000.0
    private const val GB_BITS = MB_BITS * 1000.0

    /**
     * Formats network speed based on selected [SpeedUnit].
     */
    fun formatSpeed(bytesPerSec: Long, unit: SpeedUnit): String {
        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        return when (unit) {
            SpeedUnit.BYTES_PER_SEC -> formatByteSpeed(safeBytes)
            SpeedUnit.BITS_PER_SEC -> formatBitSpeed(safeBytes)
        }
    }

    /**
     * Formats bytes per second with clean, stable thresholds.
     */
    fun formatByteSpeed(bytesPerSec: Long): String {
        val bytes = bytesPerSec.toDouble()

        return when {
            bytes >= GB_BYTES -> String.format(Locale.US, "%.2f GB/s", bytes / GB_BYTES)
            bytes >= 10.0 * MB_BYTES -> String.format(Locale.US, "%.1f MB/s", bytes / MB_BYTES)
            bytes >= MB_BYTES -> String.format(Locale.US, "%.2f MB/s", bytes / MB_BYTES)
            bytes >= 100.0 * KB_BYTES -> String.format(Locale.US, "%.0f KB/s", bytes / KB_BYTES)
            bytes >= KB_BYTES -> String.format(Locale.US, "%.1f KB/s", bytes / KB_BYTES)
            bytes > 0 -> String.format(Locale.US, "%.1f KB/s", bytes / KB_BYTES)
            else -> "0.0 KB/s"
        }
    }

    /**
     * Formats bits per second (standard telecom Mbps/Kbps rate).
     */
    fun formatBitSpeed(bytesPerSec: Long): String {
        val bits = (bytesPerSec * 8).toDouble()

        return when {
            bits >= GB_BITS -> String.format(Locale.US, "%.2f Gbps", bits / GB_BITS)
            bits >= 10.0 * MB_BITS -> String.format(Locale.US, "%.1f Mbps", bits / MB_BITS)
            bits >= MB_BITS -> String.format(Locale.US, "%.2f Mbps", bits / MB_BITS)
            bits >= 100.0 * KB_BITS -> String.format(Locale.US, "%.0f Kbps", bits / KB_BITS)
            bits >= KB_BITS -> String.format(Locale.US, "%.1f Kbps", bits / KB_BITS)
            bits > 0 -> String.format(Locale.US, "%.1f Kbps", bits / KB_BITS)
            else -> "0.0 Mbps"
        }
    }

    /**
     * Formats download speed with directional down arrow.
     */
    fun formatDownload(bytesPerSec: Long, unit: SpeedUnit): String {
        return "↓ ${formatSpeed(bytesPerSec, unit)}"
    }

    /**
     * Formats upload speed with directional up arrow.
     */
    fun formatUpload(bytesPerSec: Long, unit: SpeedUnit): String {
        return "↑ ${formatSpeed(bytesPerSec, unit)}"
    }

    /**
     * Formats combined download and upload string.
     */
    fun formatCombined(rxBytesPerSec: Long, txBytesPerSec: Long, unit: SpeedUnit): String {
        return "${formatDownload(rxBytesPerSec, unit)}  ${formatUpload(txBytesPerSec, unit)}"
    }

    /**
     * Formats compact single-string speed for status bar icon bitmaps.
     * Examples: "0K", "850K", "1.2M", "25M", "250M", "1.1G"
     */
    fun formatCompactSpeed(bytesPerSec: Long, unit: SpeedUnit): String {
        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        return when (unit) {
            SpeedUnit.BYTES_PER_SEC -> {
                when {
                    safeBytes < 1000L -> "0K"
                    safeBytes < 1_000_000L -> "${safeBytes / 1000L}K"
                    safeBytes < 10_000_000L -> String.format(Locale.US, "%.1fM", safeBytes / 1_000_000.0)
                    safeBytes < 1_000_000_000L -> "${safeBytes / 1_000_000L}M"
                    else -> String.format(Locale.US, "%.1fG", safeBytes / 1_000_000_000.0)
                }
            }
            SpeedUnit.BITS_PER_SEC -> {
                val bitsPerSec = safeBytes * 8L
                when {
                    bitsPerSec < 1000L -> "0K"
                    bitsPerSec < 1_000_000L -> "${bitsPerSec / 1000L}K"
                    bitsPerSec < 10_000_000L -> String.format(Locale.US, "%.1fM", bitsPerSec / 1_000_000.0)
                    bitsPerSec < 1_000_000_000L -> "${bitsPerSec / 1_000_000L}M"
                    else -> String.format(Locale.US, "%.1fG", bitsPerSec / 1_000_000_000.0)
                }
            }
        }
    }

    /**
     * Formats cumulative session data transfer.
     */
    fun formatDataUsage(totalBytes: Long): String {
        val bytes = if (totalBytes < 0) 0.0 else totalBytes.toDouble()
        return when {
            bytes >= GB_BYTES -> String.format(Locale.US, "%.2f GB", bytes / GB_BYTES)
            bytes >= MB_BYTES -> String.format(Locale.US, "%.1f MB", bytes / MB_BYTES)
            bytes >= KB_BYTES -> String.format(Locale.US, "%.1f KB", bytes / KB_BYTES)
            else -> String.format(Locale.US, "%d B", totalBytes)
        }
    }
}
