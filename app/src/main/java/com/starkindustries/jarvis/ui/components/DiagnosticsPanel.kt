package com.starkindustries.jarvis.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starkindustries.jarvis.ui.theme.JarvisTheme

@Composable
fun DiagnosticsPanel(
    modifier: Modifier = Modifier,
    thrusters: Float,
    repulsors: Float,
    shields: Float,
    lifeSupport: Float,
    onThrustersChange: (Float) -> Unit,
    onRepulsorsChange: (Float) -> Unit,
    onShieldsChange: (Float) -> Unit,
    onLifeSupportChange: (Float) -> Unit
) {
    val themeColors = JarvisTheme.colors

    Column(
        modifier = modifier
            .border(1.dp, themeColors.dim, RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "SYSTEM POWER RATIOS",
            color = themeColors.bright,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DiagnosticSlider(
            label = "THRUSTERS",
            value = thrusters,
            onValueChange = onThrustersChange
        )
        Spacer(modifier = Modifier.height(4.dp))
        DiagnosticSlider(
            label = "REPULSORS",
            value = repulsors,
            onValueChange = onRepulsorsChange
        )
        Spacer(modifier = Modifier.height(4.dp))
        DiagnosticSlider(
            label = "SHIELDS",
            value = shields,
            onValueChange = onShieldsChange
        )
        Spacer(modifier = Modifier.height(4.dp))
        DiagnosticSlider(
            label = "LIFE SUPPORT",
            value = lifeSupport,
            onValueChange = onLifeSupportChange
        )
    }
}

@Composable
fun DiagnosticSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val themeColors = JarvisTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = themeColors.textPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = themeColors.bright,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = themeColors.bright,
                activeTrackColor = themeColors.bright,
                inactiveTrackColor = themeColors.dark
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
