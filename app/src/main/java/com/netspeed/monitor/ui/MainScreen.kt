package com.netspeed.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netspeed.monitor.data.NetworkSpeed
import com.netspeed.monitor.data.SpeedUnit
import com.netspeed.monitor.ui.components.BatteryOptimizationCard
import com.netspeed.monitor.ui.components.LiveSpeedCard
import com.netspeed.monitor.ui.components.PermissionBanner
import com.netspeed.monitor.ui.components.UnitSelectorCard
import com.netspeed.monitor.ui.theme.CyanAccent
import com.netspeed.monitor.ui.theme.DarkBg
import com.netspeed.monitor.ui.theme.DarkSurface
import com.netspeed.monitor.ui.theme.SwitchTrackInactive
import com.netspeed.monitor.ui.theme.TextMuted
import com.netspeed.monitor.ui.theme.TextPrimary

@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    currentSpeed: NetworkSpeed,
    speedUnit: SpeedUnit,
    startOnBoot: Boolean,
    hasNotificationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onToggleService: (Boolean) -> Unit,
    onUnitSelected: (SpeedUnit) -> Unit,
    onStartOnBootChanged: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestDisableBatteryOptimization: () -> Unit
) {
    Scaffold(
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Bar with Master Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Speed Monitor",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isServiceRunning) "Running in background" else "Monitoring disabled",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = if (isServiceRunning) CyanAccent else TextMuted
                        )
                    }
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onToggleService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = CyanAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SwitchTrackInactive
                    )
                )
            }

            // Live Speed Dashboard
            LiveSpeedCard(
                isMonitoring = isServiceRunning,
                speed = currentSpeed,
                unit = speedUnit
            )

            // Permission Banner for Status Bar Notifications if missing
            if (!hasNotificationPermission) {
                PermissionBanner(
                    title = "Notification Permission Required",
                    description = "Allow notifications to display real-time speed in the status bar.",
                    onRequestPermission = onRequestNotificationPermission
                )
            }

            // Unit and Boot Preferences
            UnitSelectorCard(
                currentUnit = speedUnit,
                onUnitSelected = onUnitSelected,
                startOnBoot = startOnBoot,
                onStartOnBootChanged = onStartOnBootChanged
            )

            // Battery Optimization Guide
            BatteryOptimizationCard(
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onRequestDisableOptimization = onRequestDisableBatteryOptimization
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
