package com.netspeed.monitor

import com.netspeed.monitor.data.DisplayMode
import com.netspeed.monitor.data.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `DisplayMode active flags evaluate correctly`() {
        assertTrue(DisplayMode.NOTIFICATION_ONLY.isNotificationActive)
        assertFalse(DisplayMode.NOTIFICATION_ONLY.isOverlayActive)

        assertFalse(DisplayMode.OVERLAY_ONLY.isNotificationActive)
        assertTrue(DisplayMode.OVERLAY_ONLY.isOverlayActive)

        assertTrue(DisplayMode.BOTH.isNotificationActive)
        assertTrue(DisplayMode.BOTH.isOverlayActive)
    }

    @Test
    fun `DisplayMode fromName fallback logic`() {
        assertEquals(DisplayMode.NOTIFICATION_ONLY, DisplayMode.fromName("NOTIFICATION_ONLY"))
        assertEquals(DisplayMode.OVERLAY_ONLY, DisplayMode.fromName("OVERLAY_ONLY"))
        assertEquals(DisplayMode.BOTH, DisplayMode.fromName("BOTH"))
        assertEquals(DisplayMode.NOTIFICATION_ONLY, DisplayMode.fromName("INVALID_MODE"))
    }
}
