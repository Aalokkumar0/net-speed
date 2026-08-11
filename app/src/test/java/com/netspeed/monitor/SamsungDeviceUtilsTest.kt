package com.netspeed.monitor

import com.netspeed.monitor.utils.OneUIInfo
import com.netspeed.monitor.utils.SamsungDeviceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungDeviceUtilsTest {

    @Test
    fun `OneUIInfo data class holds expected values`() {
        val info = OneUIInfo(
            isSamsung = true,
            oneUIVersion = 6,
            oneUIMinorVersion = 1,
            androidVersion = 34,
            formattedVersion = "One UI 6.1"
        )

        assertTrue(info.isSamsung)
        assertEquals(6, info.oneUIVersion)
        assertEquals(1, info.oneUIMinorVersion)
        assertEquals(34, info.androidVersion)
        assertEquals("One UI 6.1", info.formattedVersion)
    }

    @Test
    fun `OneUIInfo non-Samsung state`() {
        val info = OneUIInfo(
            isSamsung = false,
            oneUIVersion = 0,
            oneUIMinorVersion = 0,
            androidVersion = 35,
            formattedVersion = "Android 15 (API 35)"
        )

        assertFalse(info.isSamsung)
        assertEquals(0, info.oneUIVersion)
        assertEquals(0, info.oneUIMinorVersion)
        assertEquals(35, info.androidVersion)
    }

    @Test
    fun `getOneUIInfo returns non-null result on current platform`() {
        val info = SamsungDeviceUtils.getOneUIInfo()
        assertNotNull(info)
        assertNotNull(info.formattedVersion)
    }
}
