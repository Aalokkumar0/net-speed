package com.netspeed.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netspeed.monitor.ui.theme.DarkBg
import com.netspeed.monitor.ui.theme.NetworkSpeedMonitorTheme
import com.netspeed.monitor.utils.OneUIInfo

@Preview(name = "Samsung One UI 6.1 - Optimized / Restricted", showBackground = true)
@Composable
fun BatteryOptimizationCardSamsungOneUI6RestrictedPreview() {
    NetworkSpeedMonitorTheme {
        Box(
            modifier = Modifier
                .background(DarkBg)
                .padding(16.dp)
        ) {
            BatteryOptimizationCard(
                isIgnoringBatteryOptimizations = false,
                onRequestDisableOptimization = {},
                oneUIInfo = OneUIInfo(
                    isSamsung = true,
                    oneUIVersion = 6,
                    oneUIMinorVersion = 1,
                    androidVersion = 34,
                    formattedVersion = "One UI 6.1"
                )
            )
        }
    }
}

@Preview(name = "Samsung One UI 6.1 - Unrestricted", showBackground = true)
@Composable
fun BatteryOptimizationCardSamsungOneUI6UnrestrictedPreview() {
    NetworkSpeedMonitorTheme {
        Box(
            modifier = Modifier
                .background(DarkBg)
                .padding(16.dp)
        ) {
            BatteryOptimizationCard(
                isIgnoringBatteryOptimizations = true,
                onRequestDisableOptimization = {},
                oneUIInfo = OneUIInfo(
                    isSamsung = true,
                    oneUIVersion = 6,
                    oneUIMinorVersion = 1,
                    androidVersion = 34,
                    formattedVersion = "One UI 6.1"
                )
            )
        }
    }
}

@Preview(name = "Non-Samsung Android 15 - Restricted", showBackground = true)
@Composable
fun BatteryOptimizationCardNonSamsungRestrictedPreview() {
    NetworkSpeedMonitorTheme {
        Box(
            modifier = Modifier
                .background(DarkBg)
                .padding(16.dp)
        ) {
            BatteryOptimizationCard(
                isIgnoringBatteryOptimizations = false,
                onRequestDisableOptimization = {},
                oneUIInfo = OneUIInfo(
                    isSamsung = false,
                    oneUIVersion = 0,
                    oneUIMinorVersion = 0,
                    androidVersion = 35,
                    formattedVersion = "Android 15 (API 35)"
                )
            )
        }
    }
}

@Preview(name = "Samsung One UI 5.1 - Restricted", showBackground = true)
@Composable
fun BatteryOptimizationCardSamsungOneUI5Preview() {
    NetworkSpeedMonitorTheme {
        Box(
            modifier = Modifier
                .background(DarkBg)
                .padding(16.dp)
        ) {
            BatteryOptimizationCard(
                isIgnoringBatteryOptimizations = false,
                onRequestDisableOptimization = {},
                oneUIInfo = OneUIInfo(
                    isSamsung = true,
                    oneUIVersion = 5,
                    oneUIMinorVersion = 1,
                    androidVersion = 33,
                    formattedVersion = "One UI 5.1"
                )
            )
        }
    }
}
