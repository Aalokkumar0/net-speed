package com.netspeed.monitor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netspeed.monitor.ui.theme.CyanAccent
import com.netspeed.monitor.ui.theme.DarkBg
import com.netspeed.monitor.ui.theme.DarkCardBorder
import com.netspeed.monitor.ui.theme.DarkSurface
import com.netspeed.monitor.ui.theme.DarkSurfaceVariant
import com.netspeed.monitor.ui.theme.EmeraldAccent
import com.netspeed.monitor.ui.theme.TextMuted
import com.netspeed.monitor.ui.theme.TextPrimary
import com.netspeed.monitor.ui.theme.TextSecondary

@Composable
fun BatteryOptimizationCard(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestDisableOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BACKGROUND RELIABILITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIgnoringBatteryOptimizations) Color(0x2210B981)
                            else Color(0x22F59E0B)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isIgnoringBatteryOptimizations) EmeraldAccent
                                else Color(0xFFF59E0B)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIgnoringBatteryOptimizations) "UNRESTRICTED" else "OPTIMIZED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isIgnoringBatteryOptimizations) EmeraldAccent else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIgnoringBatteryOptimizations) EmeraldAccent.copy(alpha = 0.15f)
                            else Color(0x22F59E0B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIgnoringBatteryOptimizations) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (isIgnoringBatteryOptimizations) EmeraldAccent else Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Battery Optimization",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isIgnoringBatteryOptimizations)
                            "Service is allowed to run uninterrupted in background"
                        else
                            "Samsung and other devices may kill the monitor when screen is off",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            if (!isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRequestDisableOptimization,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = DarkBg
                    )
                ) {
                    Text(
                        text = "Disable Battery Restriction",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expandable OEM Guidance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device-specific background tips (Samsung/Xiaomi)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextMuted
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• Samsung (One UI): Go to Settings → Battery → Background usage limits → Add 'Network Speed Monitor' to Never sleeping apps.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                    Text(
                        text = "• Xiaomi / POCO (HyperOS / MIUI): Enable 'Autostart' in App info and set Battery saver to 'No restrictions'.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                    Text(
                        text = "• OnePlus / Oppo / Realme: Enable 'Allow background activity' and 'Allow auto-launch'.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
