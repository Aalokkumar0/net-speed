package com.netspeed.monitor.data

/**
 * Unit of measurement for displaying network speeds.
 */
enum class SpeedUnit(val label: String, val shortLabel: String) {
    /**
     * Standard byte units (B/s, KB/s, MB/s, GB/s).
     */
    BYTES_PER_SEC("Bytes/sec (B/s, KB/s, MB/s)", "Bytes/s"),

    /**
     * Standard bit rate units (bps, Kbps, Mbps, Gbps).
     */
    BITS_PER_SEC("Bits/sec (bps, Kbps, Mbps)", "Bits/s");

    companion object {
        fun fromName(name: String?): SpeedUnit {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BYTES_PER_SEC
        }
    }
}
