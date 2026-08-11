package com.netspeed.monitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netspeed.monitor.data.DisplayMode
import com.netspeed.monitor.data.PreferenceManager
import com.netspeed.monitor.data.SpeedUnit
import com.netspeed.monitor.engine.SpeedTestManager
import com.netspeed.monitor.service.SpeedMonitorService
import com.netspeed.monitor.ui.MainScreen
import com.netspeed.monitor.ui.theme.NetworkSpeedMonitorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferenceManager: PreferenceManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (preferenceManager.getMonitoringEnabled()) {
                    SpeedMonitorService.start(this)
                }
            } else {
                Toast.makeText(
                    this,
                    "Notification permission is needed to show live speeds in status bar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val requestOverlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (checkOverlayPermission()) {
                if (preferenceManager.getMonitoringEnabled()) {
                    SpeedMonitorService.start(this)
                }
            } else {
                Toast.makeText(
                    this,
                    "Overlay permission is needed for the floating bubble",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager.getInstance(this)

        setContent {
            NetworkSpeedMonitorTheme {
                val isServiceRunning by SpeedMonitorService.isServiceRunning.collectAsState()
                val currentSpeed by SpeedMonitorService.currentSpeed.collectAsState()

                val displayMode by preferenceManager.displayMode.collectAsState()
                val speedUnit by preferenceManager.speedUnit.collectAsState()
                val startOnBoot by preferenceManager.startOnBoot.collectAsState()

                var hasNotificationPermission by remember {
                    mutableStateOf(checkNotificationPermission())
                }
                var hasOverlayPermission by remember {
                    mutableStateOf(checkOverlayPermission())
                }
                var isIgnoringBatteryOptimizations by remember {
                    mutableStateOf(checkBatteryOptimization())
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasNotificationPermission = checkNotificationPermission()
                            hasOverlayPermission = checkOverlayPermission()
                            isIgnoringBatteryOptimizations = checkBatteryOptimization()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val speedTestManager = remember { SpeedTestManager() }
                val speedTestResult by speedTestManager.testResult.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                MainScreen(
                    isServiceRunning = isServiceRunning,
                    currentSpeed = currentSpeed,
                    displayMode = displayMode,
                    speedUnit = speedUnit,
                    startOnBoot = startOnBoot,
                    hasNotificationPermission = hasNotificationPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    speedTestResult = speedTestResult,
                    onToggleService = { enable ->
                        preferenceManager.setMonitoringEnabled(enable)
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !checkNotificationPermission()
                            ) {
                                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (displayMode.isOverlayActive && !checkOverlayPermission()) {
                                requestOverlayPermission()
                            }
                            SpeedMonitorService.start(this)
                        } else {
                            SpeedMonitorService.stop(this)
                        }
                    },
                    onModeSelected = { newMode ->
                        preferenceManager.setDisplayMode(newMode)
                        if (newMode.isOverlayActive && !checkOverlayPermission()) {
                            requestOverlayPermission()
                        }
                        if (isServiceRunning) {
                            SpeedMonitorService.start(this)
                        }
                    },
                    onUnitSelected = { newUnit ->
                        preferenceManager.setSpeedUnit(newUnit)
                        if (isServiceRunning) {
                            SpeedMonitorService.start(this)
                        }
                    },
                    onStartOnBootChanged = { enable ->
                        preferenceManager.setStartOnBoot(enable)
                    },
                    onStartSpeedTest = {
                        coroutineScope.launch {
                            speedTestManager.runSpeedTest()
                        }
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestOverlayPermission = {
                        requestOverlayPermission()
                    },
                    onRequestDisableBatteryOptimization = {
                        requestDisableBatteryOptimization()
                    }
                )
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                requestOverlayPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                requestOverlayPermissionLauncher.launch(fallbackIntent)
            }
        }
    }

    private fun requestDisableBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(this, "Could not open battery settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
