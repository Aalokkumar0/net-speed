package com.netspeed.monitor.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent user preferences using SharedPreferences and exposes reactive StateFlows.
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isMonitoringEnabled = MutableStateFlow(getMonitoringEnabled())
    val isMonitoringEnabled: StateFlow<Boolean> = _isMonitoringEnabled.asStateFlow()

    private val _displayMode = MutableStateFlow(getDisplayMode())
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _speedUnit = MutableStateFlow(getSpeedUnit())
    val speedUnit: StateFlow<SpeedUnit> = _speedUnit.asStateFlow()

    private val _startOnBoot = MutableStateFlow(getStartOnBoot())
    val startOnBoot: StateFlow<Boolean> = _startOnBoot.asStateFlow()

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
        _isMonitoringEnabled.value = enabled
    }

    fun getMonitoringEnabled(): Boolean {
        return prefs.getBoolean(KEY_MONITORING_ENABLED, false)
    }

    fun setDisplayMode(mode: DisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
        _displayMode.value = mode
    }

    fun getDisplayMode(): DisplayMode {
        val name = prefs.getString(KEY_DISPLAY_MODE, DisplayMode.NOTIFICATION_ONLY.name)
        return DisplayMode.fromName(name)
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        prefs.edit().putString(KEY_SPEED_UNIT, unit.name).apply()
        _speedUnit.value = unit
    }

    fun getSpeedUnit(): SpeedUnit {
        val name = prefs.getString(KEY_SPEED_UNIT, SpeedUnit.BITS_PER_SEC.name)
        return SpeedUnit.fromName(name)
    }

    fun setStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_START_ON_BOOT, enabled).apply()
        _startOnBoot.value = enabled
    }

    fun getStartOnBoot(): Boolean {
        return prefs.getBoolean(KEY_START_ON_BOOT, true)
    }

    fun saveOverlayPosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_OVERLAY_POS_X, x)
            .putInt(KEY_OVERLAY_POS_Y, y)
            .apply()
    }

    fun getOverlayPosition(): Pair<Int, Int> {
        val x = prefs.getInt(KEY_OVERLAY_POS_X, 100)
        val y = prefs.getInt(KEY_OVERLAY_POS_Y, 200)
        return Pair(x, y)
    }

    companion object {
        private const val PREFS_NAME = "netspeed_prefs"
        private const val KEY_MONITORING_ENABLED = "key_monitoring_enabled"
        private const val KEY_DISPLAY_MODE = "key_display_mode"
        private const val KEY_SPEED_UNIT = "key_speed_unit"
        private const val KEY_START_ON_BOOT = "key_start_on_boot"
        private const val KEY_OVERLAY_POS_X = "key_overlay_pos_x"
        private const val KEY_OVERLAY_POS_Y = "key_overlay_pos_y"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context).also { instance = it }
            }
        }
    }
}
