package com.netspeed.monitor.data

/**
 * Encapsulates real-time network transfer metrics.
 *
 * @property rxBytesPerSec Bytes received per second (Download speed).
 * @property txBytesPerSec Bytes transmitted per second (Upload speed).
 * @property totalRxBytes Total cumulative bytes received since tracking started or device boot.
 * @property totalTxBytes Total cumulative bytes transmitted since tracking started or device boot.
 * @property timestamp Epoch timestamp in milliseconds of when this sample was computed.
 */
data class NetworkSpeed(
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalBytesPerSec: Long
        get() = rxBytesPerSec + txBytesPerSec

    companion object {
        val ZERO = NetworkSpeed(
            rxBytesPerSec = 0L,
            txBytesPerSec = 0L,
            totalRxBytes = 0L,
            totalTxBytes = 0L,
            timestamp = 0L
        )
    }
}
