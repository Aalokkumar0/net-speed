package com.netspeed.monitor

import com.netspeed.monitor.engine.TrafficTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficTrackerTest {

    @Test
    fun `first tick returns zero baseline without artificial spike`() {
        var currentTime = 1000L
        var rawRx = 50_000_000L
        var rawTx = 10_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )

        val firstSnapshot = tracker.sampleSnapshot()
        assertEquals(0L, firstSnapshot.downBps)
        assertEquals(0L, firstSnapshot.upBps)
        assertEquals(0L, firstSnapshot.totalRxBytes)
        assertEquals(0L, firstSnapshot.totalTxBytes)
    }

    @Test
    fun `consistent 1MBps transfer over multiple ticks returns stable speed`() {
        var currentTime = 1000L
        var rawRx = 10_000_000L
        var rawTx = 2_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )

        // Tick 1: Baseline
        tracker.sampleSnapshot()

        // Ticks 2..6: 1 MB transferred per 1000ms (1,048,576 B/s)
        val oneMb = 1024L * 1024L
        for (i in 1..5) {
            currentTime += 1000L
            rawRx += oneMb
            rawTx += 50 * 1024L

            val snapshot = tracker.sampleSnapshot()
            assertEquals(oneMb, snapshot.downBps)
            assertEquals(50 * 1024L, snapshot.upBps)
            assertEquals(i * oneMb, snapshot.totalRxBytes)
            assertEquals(i * (50 * 1024L), snapshot.totalTxBytes)
        }
    }

    @Test
    fun `counter reset or device reboot does not produce negative or garbage speed`() {
        var currentTime = 1000L
        var rawRx = 50_000_000L
        var rawTx = 10_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )

        tracker.sampleSnapshot() // Baseline

        // Normal tick
        currentTime += 1000L
        rawRx += 100_000L
        rawTx += 20_000L
        val normalSnapshot = tracker.sampleSnapshot()
        assertTrue(normalSnapshot.downBps > 0)

        // Device reboot simulation: counter resets to a much lower number
        currentTime += 1000L
        rawRx = 500L
        rawTx = 200L
        val resetSnapshot = tracker.sampleSnapshot()
        assertEquals(0L, resetSnapshot.downBps)
        assertEquals(0L, resetSnapshot.upBps)
    }

    @Test
    fun `elapsed time correction accurately calculates throughput for non-1000ms intervals`() {
        var currentTime = 1000L
        var rawRx = 10_000_000L
        var rawTx = 2_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )
        tracker.windowSize = 1 // Raw mode to test exact math

        tracker.sampleSnapshot() // Baseline

        // Simulate 1.5MB received in 1500ms -> should be exactly 1.0MB/s (1,000,000 B/s), NOT 1.5MB/s
        currentTime += 1500L
        rawRx += 1_500_000L
        rawTx += 300_000L

        val snapshot = tracker.sampleSnapshot()
        assertEquals(1_000_000L, snapshot.downBps)
        assertEquals(200_000L, snapshot.upBps)
    }

    @Test
    fun `min elapsed time guard retains previous snapshot for rapid successive ticks`() {
        var currentTime = 1000L
        var rawRx = 10_000_000L
        var rawTx = 2_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )

        tracker.sampleSnapshot() // Baseline

        // Tick at 1000ms
        currentTime += 1000L
        rawRx += 500_000L
        val snapshot1 = tracker.sampleSnapshot()
        assertEquals(500_000L, snapshot1.downBps)

        // Tick after only 50ms (rapid trigger < 200ms)
        currentTime += 50L
        rawRx += 10_000L
        val snapshot2 = tracker.sampleSnapshot()
        // Should return snapshot1 to prevent division by 50ms spike
        assertEquals(500_000L, snapshot2.downBps)
    }

    @Test
    fun `reset clears history correctly after simulated Doze gap`() {
        var currentTime = 1000L
        var rawRx = 10_000_000L
        var rawTx = 2_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )

        tracker.sampleSnapshot() // Baseline

        currentTime += 1000L
        rawRx += 100_000L
        tracker.sampleSnapshot()

        // Device went into Doze sleep for 60 seconds
        currentTime += 60_000L
        rawRx += 10_000_000L // large accumulated data over 60s
        tracker.reset()

        // First sample after reset acts as a baseline
        val afterResetSnapshot = tracker.sampleSnapshot()
        assertEquals(0L, afterResetSnapshot.downBps)
        assertEquals(0L, afterResetSnapshot.upBps)
    }

    @Test
    fun `rolling moving average absorbs batched zero-delta ticks and eliminates flicker`() {
        var currentTime = 1000L
        var rawRx = 10_000_000L
        var rawTx = 2_000_000L

        val tracker = TrafficTracker(
            timeProvider = { currentTime },
            rxBytesProvider = { rawRx },
            txBytesProvider = { rawTx }
        )
        tracker.windowSize = 3

        tracker.sampleSnapshot() // Baseline

        // Tick 1: 2 MB burst
        currentTime += 1000L
        rawRx += 2_000_000L
        val s1 = tracker.sampleSnapshot()
        assertEquals(2_000_000L, s1.downBps)

        // Tick 2: Kernel batching causes 0 bytes delta
        currentTime += 1000L
        val s2 = tracker.sampleSnapshot()
        // Average of [2_000_000, 0] = 1_000_000 B/s (NO zero flicker!)
        assertEquals(1_000_000L, s2.downBps)

        // Tick 3: Another 2 MB burst
        currentTime += 1000L
        rawRx += 2_000_000L
        val s3 = tracker.sampleSnapshot()
        // Average of [2M, 0M, 2M] = 1_333_333 B/s
        assertEquals(1_333_333L, s3.downBps)
    }
}
