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

    private val _speedUnit = MutableStateFlow(getSpeedUnit())
    val speedUnit: StateFlow<SpeedUnit> = _speedUnit.asStateFlow()

    private val _startOnBoot = MutableStateFlow(getStartOnBoot())
    val startOnBoot: StateFlow<Boolean> = _startOnBoot.asStateFlow()

    private val _dualLineIconEnabled = MutableStateFlow(getDualLineIconEnabled())
    val dualLineIconEnabled: StateFlow<Boolean> = _dualLineIconEnabled.asStateFlow()

    private val _smoothingWindowSize = MutableStateFlow(getSmoothingWindowSize())
    val smoothingWindowSize: StateFlow<Int> = _smoothingWindowSize.asStateFlow()

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
        _isMonitoringEnabled.value = enabled
    }

    fun getMonitoringEnabled(): Boolean {
        return prefs.getBoolean(KEY_MONITORING_ENABLED, false)
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

    fun setDualLineIconEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DUAL_LINE_ICON, enabled).apply()
        _dualLineIconEnabled.value = enabled
    }

    fun getDualLineIconEnabled(): Boolean {
        return prefs.getBoolean(KEY_DUAL_LINE_ICON, true)
    }

    fun setSmoothingWindowSize(size: Int) {
        val clamped = size.coerceIn(1, 5)
        prefs.edit().putInt(KEY_SMOOTHING_WINDOW_SIZE, clamped).apply()
        _smoothingWindowSize.value = clamped
    }

    fun getSmoothingWindowSize(): Int {
        return prefs.getInt(KEY_SMOOTHING_WINDOW_SIZE, 3).coerceIn(1, 5)
    }

    companion object {
        private const val PREFS_NAME = "netspeed_prefs"
        private const val KEY_MONITORING_ENABLED = "key_monitoring_enabled"
        private const val KEY_SPEED_UNIT = "key_speed_unit"
        private const val KEY_START_ON_BOOT = "key_start_on_boot"
        private const val KEY_DUAL_LINE_ICON = "key_dual_line_icon"
        private const val KEY_SMOOTHING_WINDOW_SIZE = "key_smoothing_window_size"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context).also { instance = it }
            }
        }
    }
}
