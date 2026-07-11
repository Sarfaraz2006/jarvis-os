package com.starkindustries.jarvis.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val colors = JarvisTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, colors.dim.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "SYSTEM POWER DIAGNOSTICS",
            color = colors.bright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DiagnosticSlider(
            label = "THRUSTER GIMBALS",
            value = thrusters,
            onValueChange = onThrustersChange
        )
        Spacer(modifier = Modifier.height(4.dp))
        DiagnosticSlider(
            label = "REPULSOR FIELD",
            value = repulsors,
            onValueChange = onRepulsorsChange
        )
        Spacer(modifier = Modifier.height(4.dp))
        DiagnosticSlider(
            label = "DEFLECTOR SHIELDS",
            value = shields,
            onValueChange = onShieldsChange
        )
        Spacer(modifier = Modifier.height(4.dp))
        DiagnosticSlider(
            label = "ENVIRONMENT CELLS",
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
    val colors = JarvisTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = colors.bright,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = colors.bright,
                activeTrackColor = colors.bright,
                inactiveTrackColor = colors.dark.copy(alpha = 0.6f)
            ),
            modifier = Modifier.height(20.dp)
        )
    }
}
