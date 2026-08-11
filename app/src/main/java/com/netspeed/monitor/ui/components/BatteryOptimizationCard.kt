package com.netspeed.monitor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netspeed.monitor.ui.theme.AmberWarning
import com.netspeed.monitor.ui.theme.CyanAccent
import com.netspeed.monitor.ui.theme.DarkBg
import com.netspeed.monitor.ui.theme.DarkCardBorder
import com.netspeed.monitor.ui.theme.DarkSurface
import com.netspeed.monitor.ui.theme.DarkSurfaceVariant
import com.netspeed.monitor.ui.theme.EmeraldAccent
import com.netspeed.monitor.ui.theme.TextMuted
import com.netspeed.monitor.ui.theme.TextPrimary
import com.netspeed.monitor.ui.theme.TextSecondary
import com.netspeed.monitor.utils.OneUIInfo
import com.netspeed.monitor.utils.SamsungDeviceUtils

@Composable
fun BatteryOptimizationCard(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestDisableOptimization: () -> Unit,
    modifier: Modifier = Modifier,
    oneUIInfo: OneUIInfo = remember { SamsungDeviceUtils.getOneUIInfo() }
) {
    val context = LocalContext.current
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
            // Header: Category Label + Unrestricted Status Badge
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
                                else AmberWarning
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIgnoringBatteryOptimizations) "UNRESTRICTED" else "OPTIMIZED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isIgnoringBatteryOptimizations) EmeraldAccent else AmberWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Samsung One UI / Android Platform Detection Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (oneUIInfo.isSamsung) CyanAccent.copy(alpha = 0.12f) else DarkSurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (oneUIInfo.isSamsung) CyanAccent.copy(alpha = 0.35f) else DarkCardBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = if (oneUIInfo.isSamsung) CyanAccent else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (oneUIInfo.isSamsung) {
                        "${oneUIInfo.formattedVersion} detected"
                    } else {
                        oneUIInfo.formattedVersion
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = if (oneUIInfo.isSamsung) CyanAccent else TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Info Row: Battery Icon + Description
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIgnoringBatteryOptimizations) EmeraldAccent.copy(alpha = 0.15f)
                            else AmberWarning.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIgnoringBatteryOptimizations) {
                            Icons.Default.BatteryChargingFull
                        } else {
                            Icons.Default.BatteryAlert
                        },
                        contentDescription = null,
                        tint = if (isIgnoringBatteryOptimizations) EmeraldAccent else AmberWarning,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Battery Optimization",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isIgnoringBatteryOptimizations) {
                            "Service is allowed to run uninterrupted in the background"
                        } else if (oneUIInfo.isSamsung) {
                            "Samsung One UI restricts background services when screen is off unless added to 'Never sleeping apps'"
                        } else {
                            "Android battery optimization may pause continuous background speed tracking when screen is off"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // One-Tap Direct Action Button
            Button(
                onClick = {
                    val launched = SamsungDeviceUtils.launchBatteryOptimization(context)
                    if (!launched) {
                        onRequestDisableOptimization()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (oneUIInfo.isSamsung) CyanAccent else CyanAccent,
                    contentColor = DarkBg
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DarkBg
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (oneUIInfo.isSamsung) {
                            "Open Battery Settings"
                        } else {
                            "Disable Battery Restriction"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expandable Step-by-Step Instructions Container
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { isExpanded = !isExpanded },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (oneUIInfo.isSamsung) CyanAccent else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (oneUIInfo.isSamsung) {
                                    "How to whitelist in ${oneUIInfo.formattedVersion}"
                                } else {
                                    "Background setup instructions for your phone"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(tween(200)) + expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(150)) + shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (oneUIInfo.isSamsung) {
                                SamsungInstructionsView(oneUIInfo = oneUIInfo)
                            } else {
                                NonSamsungInstructionsView()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SamsungInstructionsView(oneUIInfo: OneUIInfo) {
    val steps = when {
        oneUIInfo.oneUIVersion >= 6 -> listOf(
            "Tap \"Open Battery Settings\" above.",
            "Select \"Background usage limits\".",
            "Tap \"Never sleeping apps\".",
            "Tap \"+\" (top-right) and select \"Network Speed Monitor\"."
        )
        oneUIInfo.oneUIVersion == 5 -> listOf(
            "Tap \"Open Battery Settings\" above (Device Care → Battery).",
            "Select \"Background usage limits\" (or \"App power management\").",
            "Tap \"Never sleeping apps\".",
            "Add \"Network Speed Monitor\" to the list."
        )
        oneUIInfo.oneUIVersion in 3..4 -> listOf(
            "Go to Settings → Device Care → Battery.",
            "Select \"Background usage limits\".",
            "Tap \"Never sleeping apps\".",
            "Add \"Network Speed Monitor\"."
        )
        else -> listOf(
            "Go to Settings → Device Care → Battery → App power management.",
            "Turn off \"Put unused apps to sleep\" or add to \"Apps that won't be put to sleep\"."
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            InstructionStepRow(stepNumber = index + 1, instruction = step)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1500E5FF))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier
                    .size(14.dp)
                    .padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Adding to 'Never sleeping apps' guarantees the live status bar speed meter will never freeze or sleep on Samsung devices.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun NonSamsungInstructionsView() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InstructionStepRow(
            stepNumber = 1,
            instruction = "Stock Android / Google Pixel: Settings → Apps → Network Speed Monitor → App battery usage → Select \"Unrestricted\"."
        )
        InstructionStepRow(
            stepNumber = 2,
            instruction = "Xiaomi / POCO (HyperOS / MIUI): Enable \"Autostart\" in App Info and set Battery Saver to \"No restrictions\"."
        )
        InstructionStepRow(
            stepNumber = 3,
            instruction = "OnePlus / OPPO / Realme: Enable \"Allow background activity\" and \"Allow auto-launch\" in App Battery settings."
        )
        InstructionStepRow(
            stepNumber = 4,
            instruction = "Vivo / iQOO: Set Background power consumption to \"High background power consumption\"."
        )
    }
}

@Composable
private fun InstructionStepRow(
    stepNumber: Int,
    instruction: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(CyanAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = CyanAccent
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = instruction,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
