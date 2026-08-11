package com.netspeed.monitor

import com.netspeed.monitor.data.NetworkSpeed
import com.netspeed.monitor.data.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DataModelTest {

    @Test
    fun `SpeedUnit fromName handles valid and invalid values`() {
        assertEquals(SpeedUnit.BYTES_PER_SEC, SpeedUnit.fromName("BYTES_PER_SEC"))
        assertEquals(SpeedUnit.BITS_PER_SEC, SpeedUnit.fromName("BITS_PER_SEC"))
        assertEquals(SpeedUnit.BYTES_PER_SEC, SpeedUnit.fromName("UNKNOWN"))
        assertEquals(SpeedUnit.BYTES_PER_SEC, SpeedUnit.fromName(null))
    }

    @Test
    fun `NetworkSpeed ZERO constant has zero values`() {
        assertEquals(0L, NetworkSpeed.ZERO.rxBytesPerSec)
        assertEquals(0L, NetworkSpeed.ZERO.txBytesPerSec)
        assertEquals(0L, NetworkSpeed.ZERO.totalRxBytes)
        assertEquals(0L, NetworkSpeed.ZERO.totalTxBytes)
        assertNotNull(NetworkSpeed.ZERO.timestamp)
    }

    @Test
    fun `NetworkSpeed custom instantiation`() {
        val speed = NetworkSpeed(
            rxBytesPerSec = 1024L,
            txBytesPerSec = 512L,
            totalRxBytes = 2048L,
            totalTxBytes = 1024L,
            timestamp = 123456789L
        )
        assertEquals(1024L, speed.rxBytesPerSec)
        assertEquals(512L, speed.txBytesPerSec)
        assertEquals(2048L, speed.totalRxBytes)
        assertEquals(1024L, speed.totalTxBytes)
        assertEquals(123456789L, speed.timestamp)
    }
}
