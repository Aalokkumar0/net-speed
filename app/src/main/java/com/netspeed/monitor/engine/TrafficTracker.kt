package com.netspeed.monitor.engine

import android.net.TrafficStats
import android.os.SystemClock
import android.util.Log
import com.netspeed.monitor.data.NetworkSpeed

/**
 * Immutable snapshot of calculated download and upload throughput.
 */
data class SpeedSnapshot(
    val downBps: Long,
    val upBps: Long,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts to the existing [NetworkSpeed] model for UI compatibility.
     */
    fun toNetworkSpeed(): NetworkSpeed = NetworkSpeed(
        rxBytesPerSec = downBps,
        txBytesPerSec = upBps,
        totalRxBytes = totalRxBytes,
        totalTxBytes = totalTxBytes,
        timestamp = timestamp
    )

    companion object {
        val ZERO = SpeedSnapshot(
            downBps = 0L,
            upBps = 0L,
            totalRxBytes = 0L,
            totalTxBytes = 0L,
            timestamp = 0L
        )
    }
}

/**
 * High-precision, flicker-free network throughput tracker.
 *
 * =========================================================================================
 * WHY THIS ELIMINATES STATUS BAR SPEED FLICKER:
 * 1. Real Elapsed Time Division:
 *    Instead of assuming each tick is exactly 1000ms, we measure exact delta time using
 *    [SystemClock.elapsedRealtime()]. Throughput is calculated as (deltaBytes * 1000) / elapsedMs.
 *    This prevents CPU scheduling jitter or GC pauses from artificially skewing numbers.
 *
 * 2. Rolling Average Smoothing Buffer (ArrayDeque):
 *    Linux/Android kernel [TrafficStats] socket counters frequently update in batches. A 1-second
 *    read may capture 0 bytes followed by a 2-second buffer burst. A rolling moving average
 *    (default window = 3) absorbs these batching artifacts and eliminates 0-speed flickering.
 *
 * 3. Min Elapsed Time Guard (200ms):
 *    If two ticks occur within < 200ms, the previous smoothed snapshot is retained, avoiding
 *    divide-by-near-zero rate explosions.
 *
 * 4. Doze / Suspend Gap Detection (> 5000ms):
 *    If the device enters deep sleep and wakes up seconds/minutes later, a raw delta would
 *    compute a massive false throughput spike. We detect gaps > 5000ms and re-baseline cleanly.
 * =========================================================================================
 */
class TrafficTracker(
    private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() },
    private val rxBytesProvider: () -> Long = { TrafficStats.getTotalRxBytes() },
    private val txBytesProvider: () -> Long = { TrafficStats.getTotalTxBytes() }
) {

    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L
    private var lastSampleTimeMs: Long = 0L

    private var sessionStartRxBytes: Long = -1L
    private var sessionStartTxBytes: Long = -1L

    // Pre-allocated sliding window buffers to avoid garbage collection allocations per tick
    private val rxWindow = ArrayDeque<Long>(MAX_WINDOW_SIZE)
    private val txWindow = ArrayDeque<Long>(MAX_WINDOW_SIZE)

    private var lastSnapshot: SpeedSnapshot = SpeedSnapshot.ZERO

    /**
     * Rolling average window size (1 = raw instantaneous, 3 = balanced default, 5 = maximum smooth).
     */
    var windowSize: Int = DEFAULT_WINDOW_SIZE
        set(value) {
            field = value.coerceIn(MIN_WINDOW_SIZE, MAX_WINDOW_SIZE)
            trimWindows()
        }

    /**
     * Clears tracking history and resets baseline counters.
     * Call this when the app resumes after a long pause or when monitoring restarts.
     */
    @Synchronized
    fun reset() {
        lastRxBytes = -1L
        lastTxBytes = -1L
        lastSampleTimeMs = 0L
        sessionStartRxBytes = -1L
        sessionStartTxBytes = -1L
        rxWindow.clear()
        txWindow.clear()
        lastSnapshot = SpeedSnapshot.ZERO
        Log.d(TAG, "[RESET] TrafficTracker baselines and smoothing buffers cleared.")
    }

    /**
     * Samples the network throughput delta since the last tick and returns a smoothed [SpeedSnapshot].
     */
    @Synchronized
    fun sampleSnapshot(): SpeedSnapshot {
        val currentTimeMs = timeProvider()

        val rawRx = rxBytesProvider()
        val rawTx = txBytesProvider()

        val currentRx: Long = if (rawRx == TrafficStats.UNSUPPORTED.toLong() || rawRx < 0L) 0L else rawRx
        val currentTx: Long = if (rawTx == TrafficStats.UNSUPPORTED.toLong() || rawTx < 0L) 0L else rawTx

        // Initialize session total baselines
        if (sessionStartRxBytes == -1L) sessionStartRxBytes = currentRx
        if (sessionStartTxBytes == -1L) sessionStartTxBytes = currentTx

        // Edge Case 1: First tick initialization — record baseline and return 0
        if (lastRxBytes == -1L || lastTxBytes == -1L || lastSampleTimeMs == 0L) {
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastSampleTimeMs = currentTimeMs
            rxWindow.clear()
            txWindow.clear()
            lastSnapshot = SpeedSnapshot(
                downBps = 0L,
                upBps = 0L,
                totalRxBytes = 0L,
                totalTxBytes = 0L,
                timestamp = System.currentTimeMillis()
            )
            Log.d(TAG, "[BASELINE] First tick initialized: rawRx=$currentRx, rawTx=$currentTx")
            return lastSnapshot
        }

        val elapsedMs: Long = currentTimeMs - lastSampleTimeMs

        // Edge Case 2: Guard against rapid successive ticks (< MIN_ELAPSED_MS)
        if (elapsedMs < MIN_ELAPSED_MS) {
            Log.d(TAG, "[GUARD] Tick fired too quickly (${elapsedMs}ms < ${MIN_ELAPSED_MS}ms). Returning previous snapshot.")
            return lastSnapshot
        }

        // Edge Case 3: Doze mode / screen-off sleep gap detection (> MAX_ALLOWABLE_GAP_MS)
        // If elapsed time is huge, re-baseline to avoid a massive artificial throughput burst
        if (elapsedMs > MAX_ALLOWABLE_GAP_MS) {
            Log.d(TAG, "[GAP_DETECTED] Large elapsed gap (${elapsedMs}ms > ${MAX_ALLOWABLE_GAP_MS}ms). Resetting tick baseline.")
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastSampleTimeMs = currentTimeMs
            rxWindow.clear()
            txWindow.clear()
            return lastSnapshot
        }

        // Edge Case 4: Counter reset / device reboot / integer rollover
        val rxDelta: Long = if (currentRx >= lastRxBytes) {
            currentRx - lastRxBytes
        } else {
            Log.w(TAG, "[COUNTER_ROLLOVER] Rx counter reset detected: current=$currentRx < last=$lastRxBytes")
            sessionStartRxBytes = currentRx
            rxWindow.clear()
            0L
        }

        val txDelta: Long = if (currentTx >= lastTxBytes) {
            currentTx - lastTxBytes
        } else {
            Log.w(TAG, "[COUNTER_ROLLOVER] Tx counter reset detected: current=$currentTx < last=$lastTxBytes")
            sessionStartTxBytes = currentTx
            txWindow.clear()
            0L
        }

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastSampleTimeMs = currentTimeMs

        // Exact instantaneous throughput: (deltaBytes * 1000.0) / elapsedMs
        val elapsedSeconds: Double = elapsedMs.toDouble() / 1000.0
        val instantaneousRx: Long = if (elapsedSeconds > 0.0) {
            ((rxDelta.toDouble() * 1000.0) / elapsedMs).toLong()
        } else 0L

        val instantaneousTx: Long = if (elapsedSeconds > 0.0) {
            ((txDelta.toDouble() * 1000.0) / elapsedMs).toLong()
        } else 0L

        // Push instantaneous rates into rolling window queues
        pushToWindow(rxWindow, instantaneousRx)
        pushToWindow(txWindow, instantaneousTx)

        // Compute moving average across the window
        val smoothedRx = calculateAverage(rxWindow)
        val smoothedTx = calculateAverage(txWindow)

        val sessionTotalRx: Long = if (sessionStartRxBytes != -1L && currentRx >= sessionStartRxBytes) {
            currentRx - sessionStartRxBytes
        } else 0L

        val sessionTotalTx: Long = if (sessionStartTxBytes != -1L && currentTx >= sessionStartTxBytes) {
            currentTx - sessionStartTxBytes
        } else 0L

        lastSnapshot = SpeedSnapshot(
            downBps = smoothedRx,
            upBps = smoothedTx,
            totalRxBytes = sessionTotalRx,
            totalTxBytes = sessionTotalTx,
            timestamp = System.currentTimeMillis()
        )

        Log.d(
            TAG,
            "[TICK] elapsed=${elapsedMs}ms | rxDelta=$rxDelta, txDelta=$txDelta | " +
            "instantRx=$instantaneousRx -> smoothedRx=$smoothedRx B/s | " +
            "instantTx=$instantaneousTx -> smoothedTx=$smoothedTx B/s (windowSize=$windowSize)"
        )

        return lastSnapshot
    }

    /**
     * Legacy sample method returning [NetworkSpeed] for seamless compatibility.
     */
    @Synchronized
    fun sample(): NetworkSpeed {
        return sampleSnapshot().toNetworkSpeed()
    }

    private fun pushToWindow(window: ArrayDeque<Long>, value: Long) {
        while (window.size >= windowSize) {
            window.removeFirst()
        }
        window.addLast(value)
    }

    private fun calculateAverage(window: ArrayDeque<Long>): Long {
        if (window.isEmpty()) return 0L
        var sum = 0L
        for (v in window) {
            sum += v
        }
        return sum / window.size
    }

    private fun trimWindows() {
        while (rxWindow.size > windowSize) rxWindow.removeFirst()
        while (txWindow.size > windowSize) txWindow.removeFirst()
    }

    companion object {
        private const val TAG = "SpeedDebug"
        const val DEFAULT_WINDOW_SIZE = 3
        const val MIN_WINDOW_SIZE = 1
        const val MAX_WINDOW_SIZE = 5
        const val MIN_ELAPSED_MS = 200L
        const val MAX_ALLOWABLE_GAP_MS = 5000L
    }
}
