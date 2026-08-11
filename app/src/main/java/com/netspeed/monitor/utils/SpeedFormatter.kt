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
     * Formats a number-only speed string for status bar icon bitmaps.
     * Returns ONLY the numeric value with NO unit suffix (K/M/G), max 3 characters.
     * Unit context is shown in the expanded notification body text instead.
     *
     * Examples: "0", "500", "1.2", "25", "250", "999"
     *
     * The value represents KB/s or Kbps depending on [unit], transitioning to
     * MB/s or Mbps scale at >= 1M, capped at "999" for GB+ speeds.
     */
    fun formatCompactSpeed(bytesPerSec: Long, unit: SpeedUnit): String {
        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        val value: Double = when (unit) {
            SpeedUnit.BYTES_PER_SEC -> safeBytes.toDouble()
            SpeedUnit.BITS_PER_SEC -> (safeBytes * 8).toDouble()
        }

        return when {
            // Below 1 K → show "0"
            value < 1_000.0 -> "0"
            // 1 K .. 999 K → whole number KB/Kbps: "1" to "999"
            value < 1_000_000.0 -> "${(value / 1_000.0).toLong()}"
            // 1.0 M .. 9.9 M → one decimal: "1.0" to "9.9" (3 chars)
            // Threshold 9_950_000 ensures %.1f never rounds up to "10.0"
            value < 9_950_000.0 -> String.format(Locale.US, "%.1f", value / 1_000_000.0)
            // 10 M .. 999 M → whole number: "10" to "999" (2-3 chars)
            value < 1_000_000_000.0 -> {
                val rounded = kotlin.math.round(value / 1_000_000.0).toLong()
                "${rounded.coerceAtMost(999)}"
            }
            // 1 G+ → cap at "999"
            else -> "999"
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
