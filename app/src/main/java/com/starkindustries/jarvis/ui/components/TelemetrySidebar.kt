package com.starkindustries.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starkindustries.jarvis.ui.theme.JarvisColors
import com.starkindustries.jarvis.ui.theme.ThemeMode

@Composable
fun TelemetrySidebar(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    batteryPercent: Int,
    isCharging: Boolean,
    logs: List<TelemetryLog>,
    thrusters: Float,
    repulsors: Float,
    shields: Float,
    lifeSupport: Float,
    onThrustersChange: (Float) -> Unit,
    onRepulsorsChange: (Float) -> Unit,
    onShieldsChange: (Float) -> Unit,
    onLifeSupportChange: (Float) -> Unit,
    flashlightState: Boolean,
    onFlashlightToggle: (Boolean) -> Unit,
    colors: JarvisColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battery status cell and quick toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .border(1.dp, colors.dim.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, colors.dim.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ArcReactorHud(
                    modifier = Modifier.fillMaxSize(),
                    powerPercent = (thrusters * 25 + repulsors * 25 + shields * 25 + lifeSupport * 25).toInt(),
                    onClick = {
                        onFlashlightToggle(!flashlightState)
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ENERGY CORE STATUS",
                    color = colors.textSecondary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$batteryPercent% ${if (isCharging) "SYNCED" else "STEADY"}",
                    color = colors.bright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "FLASHLIGHT: ${if (flashlightState) "ENABLED" else "MUTED"}",
                    color = colors.bright.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onFlashlightToggle(!flashlightState) }
                )
            }
        }

        // Diagnostics Slider Ratios
        DiagnosticsPanel(
            modifier = Modifier.fillMaxWidth(),
            thrusters = thrusters,
            repulsors = repulsors,
            shields = shields,
            lifeSupport = lifeSupport,
            onThrustersChange = onThrustersChange,
            onRepulsorsChange = onRepulsorsChange,
            onShieldsChange = onShieldsChange,
            onLifeSupportChange = onLifeSupportChange
        )

        // Theme Sync Module
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .border(1.dp, colors.dim.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "INTELLIGENCE MATRIX SYNC",
                color = colors.bright,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SidebarThemeButton(
                label = "J.A.R.V.I.S.",
                active = themeMode == ThemeMode.JARVIS,
                onClick = { onThemeChange(ThemeMode.JARVIS) },
                color = colors.bright
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarThemeButton(
                label = "F.R.I.D.A.Y.",
                active = themeMode == ThemeMode.FRIDAY,
                onClick = { onThemeChange(ThemeMode.FRIDAY) },
                color = colors.bright
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarThemeButton(
                label = "SAFETY MODE",
                active = themeMode == ThemeMode.SAFETY,
                onClick = { onThemeChange(ThemeMode.SAFETY) },
                color = colors.bright
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarThemeButton(
                label = "RED ALERT",
                active = themeMode == ThemeMode.RED_ALERT,
                onClick = { onThemeChange(ThemeMode.RED_ALERT) },
                color = colors.bright
            )
        }

        // Logs panel
        TelemetryFeed(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            logs = logs
        )
    }
}

@Composable
fun SidebarThemeButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) color.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (active) color else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (active) color else color.copy(alpha = 0.2f),
                RoundedCornerShape(6.dp)
            ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
    }
}
