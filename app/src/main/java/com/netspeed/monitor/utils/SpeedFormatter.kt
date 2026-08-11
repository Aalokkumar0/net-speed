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
     * Formats a compact string (max <= 4 characters) suitable for status bar icons.
     * Includes abbreviated unit suffix (K/M/G) for unambiguous stacked readings.
     *
     * Examples: "0", "45K", "850K", "1.2M", "9.9M", "10M", "25M", "999M", "1.5G"
     *
     * Features strict boundary protection:
     * - Threshold 9_950_000 ensures %.1f never rounds up to "10.0" (which would be 4 chars + unit).
     *   Instead, values >= 9.95M transition cleanly to integer "10M" (3 chars).
     */
    fun formatCompactSpeed(bytesPerSec: Long, unit: SpeedUnit = SpeedUnit.BYTES_PER_SEC): String {
        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        val value: Double = when (unit) {
            SpeedUnit.BYTES_PER_SEC -> safeBytes.toDouble()
            SpeedUnit.BITS_PER_SEC -> (safeBytes * 8).toDouble()
        }

        return when {
            // Below 1 K → "0"
            value < 1_000.0 -> "0"

            // 1 K .. 999 K → "1K" to "999K" (2-4 chars)
            value < 1_000_000.0 -> "${(value / 1_000.0).toLong()}K"

            // 1.0 M .. 9.9 M → "1.0M" to "9.9M" (4 chars)
            // Boundary: 9_950_000 is used so String.format doesn't round 9.96M to "10.0M" (5 chars)
            value < 9_950_000.0 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)

            // 10 M .. 999 M → "10M" to "999M" (3-4 chars)
            value < 1_000_000_000.0 -> {
                val rounded = kotlin.math.round(value / 1_000_000.0).toLong()
                "${rounded.coerceAtMost(999)}M"
            }

            // 1.0 G .. 9.9 G → "1.0G" to "9.9G" (4 chars)
            value < 9_950_000_000.0 -> String.format(Locale.US, "%.1fG", value / 1_000_000_000.0)

            // 10 G+ → "999M" / max cap
            else -> "999M"
        }
    }

    /**
     * Formats number-only speed string (without unit suffix, max 3 chars) for single-line icon fallback.
     */
    fun formatNumberOnlySpeed(bytesPerSec: Long, unit: SpeedUnit = SpeedUnit.BYTES_PER_SEC): String {
        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        val value: Double = when (unit) {
            SpeedUnit.BYTES_PER_SEC -> safeBytes.toDouble()
            SpeedUnit.BITS_PER_SEC -> (safeBytes * 8).toDouble()
        }

        return when {
            value < 1_000.0 -> "0"
            value < 1_000_000.0 -> "${(value / 1_000.0).toLong()}"
            value < 9_950_000.0 -> String.format(Locale.US, "%.1f", value / 1_000_000.0)
            value < 1_000_000_000.0 -> {
                val rounded = kotlin.math.round(value / 1_000_000.0).toLong()
                "${rounded.coerceAtMost(999)}"
            }
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

    /**
     * Formats separate session download and upload data usage.
     */
    fun formatDetailedDataUsage(rxBytes: Long, txBytes: Long): String {
        return "Session: ↓ ${formatDataUsage(rxBytes)}  ↑ ${formatDataUsage(txBytes)}"
    }
}
