package com.netspeed.monitor

import com.netspeed.monitor.data.SpeedUnit
import com.netspeed.monitor.utils.SpeedFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedFormatterTest {

    @Test
    fun `formatByteSpeed formats bytes correctly`() {
        assertEquals("0.0 KB/s", SpeedFormatter.formatByteSpeed(0L))
        assertEquals("0.5 KB/s", SpeedFormatter.formatByteSpeed(512L))
        assertEquals("1.0 KB/s", SpeedFormatter.formatByteSpeed(1024L))
        assertEquals("1.5 KB/s", SpeedFormatter.formatByteSpeed(1536L))
        assertEquals("450 KB/s", SpeedFormatter.formatByteSpeed(450 * 1024L))
        assertEquals("1.00 MB/s", SpeedFormatter.formatByteSpeed(1024L * 1024L))
        assertEquals("2.50 MB/s", SpeedFormatter.formatByteSpeed((2.5 * 1024 * 1024).toLong()))
        assertEquals("25.0 MB/s", SpeedFormatter.formatByteSpeed((25.0 * 1024 * 1024).toLong()))
        assertEquals("1.00 GB/s", SpeedFormatter.formatByteSpeed(1024L * 1024L * 1024L))
    }

    @Test
    fun `formatBitSpeed formats bits correctly`() {
        assertEquals("0.0 Mbps", SpeedFormatter.formatBitSpeed(0L))
        // 1000 bytes = 8000 bits = 8.0 Kbps
        assertEquals("8.0 Kbps", SpeedFormatter.formatBitSpeed(1000L))
        // 125,000 bytes = 1,000,000 bits = 1.00 Mbps
        assertEquals("1.00 Mbps", SpeedFormatter.formatBitSpeed(125_000L))
        // 12,500,000 bytes = 100,000,000 bits = 100.0 Mbps
        assertEquals("100.0 Mbps", SpeedFormatter.formatBitSpeed(12_500_000L))
    }

    @Test
    fun `formatCombined outputs both directions with directional arrows`() {
        val result = SpeedFormatter.formatCombined(
            rxBytesPerSec = 1024L * 1024L,
            txBytesPerSec = 45 * 1024L,
            unit = SpeedUnit.BYTES_PER_SEC
        )
        assertTrue(result.contains("↓ 1.00 MB/s"))
        assertTrue(result.contains("↑ 45.0 KB/s"))
    }

    @Test
    fun `formatDataUsage and formatDetailedDataUsage format cumulative usage`() {
        assertEquals("0 B", SpeedFormatter.formatDataUsage(0L))
        assertEquals("1.0 KB", SpeedFormatter.formatDataUsage(1024L))
        assertEquals("10.0 MB", SpeedFormatter.formatDataUsage(10 * 1024 * 1024L))
        assertEquals("1.50 GB", SpeedFormatter.formatDataUsage((1.5 * 1024 * 1024 * 1024).toLong()))

        val detailed = SpeedFormatter.formatDetailedDataUsage(10 * 1024 * 1024L, 1024L)
        assertTrue(detailed.contains("↓ 10.0 MB"))
        assertTrue(detailed.contains("↑ 1.0 KB"))
    }

    @Test
    fun `formatCompactSpeed formats status bar strings correctly with unit suffix`() {
        // Bytes per sec
        assertEquals("0", SpeedFormatter.formatCompactSpeed(0L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("45K", SpeedFormatter.formatCompactSpeed(45_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("500K", SpeedFormatter.formatCompactSpeed(500_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("1.2M", SpeedFormatter.formatCompactSpeed(1_240_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("9.9M", SpeedFormatter.formatCompactSpeed(9_940_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("10M", SpeedFormatter.formatCompactSpeed(9_950_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("25M", SpeedFormatter.formatCompactSpeed(25_000_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("250M", SpeedFormatter.formatCompactSpeed(250_000_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("1.5G", SpeedFormatter.formatCompactSpeed(1_500_000_000L, SpeedUnit.BYTES_PER_SEC))

        // Bits per sec
        assertEquals("0", SpeedFormatter.formatCompactSpeed(0L, SpeedUnit.BITS_PER_SEC))
        assertEquals("400K", SpeedFormatter.formatCompactSpeed(50_000L, SpeedUnit.BITS_PER_SEC)) // 50KB = 400Kb
        assertEquals("9.6M", SpeedFormatter.formatCompactSpeed(1_200_000L, SpeedUnit.BITS_PER_SEC)) // 1.2MB = 9.6Mb
        assertEquals("200M", SpeedFormatter.formatCompactSpeed(25_000_000L, SpeedUnit.BITS_PER_SEC)) // 25MB = 200Mb
    }

    @Test
    fun `formatCompactSpeed never exceeds 4 characters`() {
        // Boundary: 9_950_000 should produce "10M" (3 chars), not "10.0M" (5 chars)
        assertEquals("10M", SpeedFormatter.formatCompactSpeed(9_950_000L, SpeedUnit.BYTES_PER_SEC))
        // Just below boundary: should produce "9.9M" (4 chars)
        assertEquals("9.9M", SpeedFormatter.formatCompactSpeed(9_940_000L, SpeedUnit.BYTES_PER_SEC))

        // Verify all outputs are <= 4 chars
        val testValues = listOf(
            0L, 500L, 5_000L, 50_000L, 500_000L, 1_240_000L, 9_940_000L,
            9_950_000L, 25_000_000L, 250_000_000L, 999_000_000L, 1_500_000_000L, 9_940_000_000L, 15_000_000_000L
        )
        for (v in testValues) {
            val result = SpeedFormatter.formatCompactSpeed(v, SpeedUnit.BYTES_PER_SEC)
            assertTrue("formatCompactSpeed($v) = \"$result\" exceeds 4 chars", result.length <= 4)
        }
    }

    @Test
    fun `formatNumberOnlySpeed never exceeds 3 characters`() {
        assertEquals("0", SpeedFormatter.formatNumberOnlySpeed(0L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("500", SpeedFormatter.formatNumberOnlySpeed(500_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("1.2", SpeedFormatter.formatNumberOnlySpeed(1_240_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("10", SpeedFormatter.formatNumberOnlySpeed(9_950_000L, SpeedUnit.BYTES_PER_SEC))
        assertEquals("999", SpeedFormatter.formatNumberOnlySpeed(1_500_000_000L, SpeedUnit.BYTES_PER_SEC))
    }
}
