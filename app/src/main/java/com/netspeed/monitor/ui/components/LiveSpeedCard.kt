package com.netspeed.monitor.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netspeed.monitor.data.NetworkSpeed
import com.netspeed.monitor.data.SpeedUnit
import com.netspeed.monitor.ui.theme.CyanAccent
import com.netspeed.monitor.ui.theme.CyanAccentGlow
import com.netspeed.monitor.ui.theme.DarkCardBorder
import com.netspeed.monitor.ui.theme.DarkSurface
import com.netspeed.monitor.ui.theme.DarkSurfaceVariant
import com.netspeed.monitor.ui.theme.EmeraldAccent
import com.netspeed.monitor.ui.theme.EmeraldAccentGlow
import com.netspeed.monitor.ui.theme.TextMuted
import com.netspeed.monitor.ui.theme.TextPrimary
import com.netspeed.monitor.ui.theme.TextSecondary
import com.netspeed.monitor.utils.SpeedFormatter

@Composable
fun LiveSpeedCard(
    isMonitoring: Boolean,
    speed: NetworkSpeed,
    unit: SpeedUnit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val safeRx = if (isMonitoring) speed.rxBytesPerSec else 0L
    val safeTx = if (isMonitoring) speed.txBytesPerSec else 0L

    val alternateUnit = if (unit == SpeedUnit.BITS_PER_SEC) SpeedUnit.BYTES_PER_SEC else SpeedUnit.BITS_PER_SEC

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
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE THROUGHPUT",
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
                        .background(if (isMonitoring) Color(0x2210B981) else Color(0x2264748B))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .then(if (isMonitoring) Modifier.scale(pulseScale) else Modifier)
                            .clip(CircleShape)
                            .background(if (isMonitoring) EmeraldAccent else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMonitoring) "ACTIVE" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isMonitoring) EmeraldAccent else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Two Columns: Download and Upload Meters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download Metric Tile
                SpeedMeterTile(
                    title = "DOWNLOAD",
                    speedText = SpeedFormatter.formatSpeed(safeRx, unit),
                    altSpeedText = SpeedFormatter.formatSpeed(safeRx, alternateUnit),
                    icon = Icons.Default.ArrowDownward,
                    accentColor = CyanAccent,
                    glowColor = CyanAccentGlow,
                    modifier = Modifier.weight(1f)
                )

                // Upload Metric Tile
                SpeedMeterTile(
                    title = "UPLOAD",
                    speedText = SpeedFormatter.formatSpeed(safeTx, unit),
                    altSpeedText = SpeedFormatter.formatSpeed(safeTx, alternateUnit),
                    icon = Icons.Default.ArrowUpward,
                    accentColor = EmeraldAccent,
                    glowColor = EmeraldAccentGlow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session Data Usage Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DataUsage,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Session Data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Text(
                    text = SpeedFormatter.formatDataUsage(speed.totalRxBytes + speed.totalTxBytes),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SpeedMeterTile(
    title: String,
    speedText: String,
    altSpeedText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        glowColor,
                        DarkSurfaceVariant
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = speedText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = accentColor
            )

            Text(
                text = "($altSpeedText)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp
                ),
                color = TextMuted
            )
        }
    }
}
