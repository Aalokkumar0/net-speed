package com.netspeed.monitor.engine

import android.net.TrafficStats
import android.os.SystemClock
import android.util.Log
import com.netspeed.monitor.data.NetworkSpeed

/**
 * Single source of truth for network throughput calculation.
 * Measures total device network traffic using TrafficStats.getTotalRxBytes()
 * and TrafficStats.getTotalTxBytes().
 */
class TrafficTracker {

    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L
    private var lastSampleTimeMs: Long = 0L

    private var sessionStartRxBytes: Long = -1L
    private var sessionStartTxBytes: Long = -1L

    @Synchronized
    fun reset() {
        lastRxBytes = -1L
        lastTxBytes = -1L
        lastSampleTimeMs = 0L
        sessionStartRxBytes = -1L
        sessionStartTxBytes = -1L
    }

    /**
     * Samples the network traffic delta since the last call and calculates
     * throughput using floating-point division.
     */
    @Synchronized
    fun sample(): NetworkSpeed {
        val currentTimeMs = SystemClock.elapsedRealtime()

        // Read total bytes across all interfaces
        val rawRx = TrafficStats.getTotalRxBytes()
        val rawTx = TrafficStats.getTotalTxBytes()

        val currentRx: Long = if (rawRx == TrafficStats.UNSUPPORTED.toLong() || rawRx < 0L) 0L else rawRx
        val currentTx: Long = if (rawTx == TrafficStats.UNSUPPORTED.toLong() || rawTx < 0L) 0L else rawTx

        // Establish session baseline if not set
        if (sessionStartRxBytes == -1L) sessionStartRxBytes = currentRx
        if (sessionStartTxBytes == -1L) sessionStartTxBytes = currentTx

        // Edge case 1: First-tick baseline - emit 0 without computing delta
        if (lastRxBytes == -1L || lastTxBytes == -1L || lastSampleTimeMs == 0L) {
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastSampleTimeMs = currentTimeMs
            Log.d(
                TAG,
                "[BASELINE] rawRxBytes=$currentRx (Long), rawTxBytes=$currentTx (Long) | first-tick initialized"
            )
            return NetworkSpeed(
                rxBytesPerSec = 0L,
                txBytesPerSec = 0L,
                totalRxBytes = 0L,
                totalTxBytes = 0L,
                timestamp = System.currentTimeMillis()
            )
        }

        val elapsedMs: Long = currentTimeMs - lastSampleTimeMs
        val elapsedSeconds: Double = elapsedMs.toDouble() / 1000.0

        // Edge case 2: Counter reset/rollover/reboot - if current < previous, re-baseline
        val rxDelta: Long = if (currentRx >= lastRxBytes) {
            currentRx - lastRxBytes
        } else {
            sessionStartRxBytes = currentRx
            0L
        }

        val txDelta: Long = if (currentTx >= lastTxBytes) {
            currentTx - lastTxBytes
        } else {
            sessionStartTxBytes = currentTx
            0L
        }

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastSampleTimeMs = currentTimeMs

        // Core math: bytesDelta.toDouble() / elapsedSeconds (never integer division)
        val rxSpeedDouble: Double = if (elapsedSeconds > 0.0) rxDelta.toDouble() / elapsedSeconds else 0.0
        val txSpeedDouble: Double = if (elapsedSeconds > 0.0) txDelta.toDouble() / elapsedSeconds else 0.0

        val rxSpeedBytesPerSec: Long = rxSpeedDouble.toLong()
        val txSpeedBytesPerSec: Long = txSpeedDouble.toLong()

        val sessionTotalRx: Long = if (sessionStartRxBytes != -1L && currentRx >= sessionStartRxBytes) {
            currentRx - sessionStartRxBytes
        } else 0L

        val sessionTotalTx: Long = if (sessionStartTxBytes != -1L && currentTx >= sessionStartTxBytes) {
            currentTx - sessionStartTxBytes
        } else 0L

        Log.d(
            TAG,
            "[TICK] rawRxBytes=$currentRx (Long), rawTxBytes=$currentTx (Long) | " +
            "rxDelta=$rxDelta (Long), txDelta=$txDelta (Long) | " +
            "elapsedSeconds=$elapsedSeconds (Double) | " +
            "computedRxDouble=$rxSpeedDouble (Double) -> rxSpeedLong=$rxSpeedBytesPerSec (Long, B/s) | " +
            "computedTxDouble=$txSpeedDouble (Double) -> txSpeedLong=$txSpeedBytesPerSec (Long, B/s)"
        )

        return NetworkSpeed(
            rxBytesPerSec = rxSpeedBytesPerSec,
            txBytesPerSec = txSpeedBytesPerSec,
            totalRxBytes = sessionTotalRx,
            totalTxBytes = sessionTotalTx,
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        private const val TAG = "SpeedDebug"
    }
}
