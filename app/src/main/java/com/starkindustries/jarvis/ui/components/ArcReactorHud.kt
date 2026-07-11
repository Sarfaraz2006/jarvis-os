package com.starkindustries.jarvis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.starkindustries.jarvis.ui.theme.JarvisTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorHud(
    modifier: Modifier = Modifier,
    powerPercent: Int = 100,
    onClick: () -> Unit = {}
) {
    val themeColors = JarvisTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

    // Slow counter-clockwise outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Fast clockwise inner ring rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    // Pulsing core scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Expandable radar waves
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    Box(
        modifier = modifier.clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width.coerceAtMost(size.height) / 2.8f
            val themeBright = themeColors.bright
            val themeGlow = themeColors.glow
            val themeDim = themeColors.dim

            // 1. Draw Radar Pulse Waves
            val maxWaveRadius = baseRadius * 1.4f
            val currentWaveRadius = baseRadius * pulseScale + (maxWaveRadius - baseRadius) * waveProgress
            val waveAlpha = (1f - waveProgress) * 0.4f
            drawCircle(
                color = themeBright.copy(alpha = waveAlpha),
                radius = currentWaveRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            // 2. Draw Outer Glowing Dashed Ring (Slow counter rotation)
            rotate(degrees = outerRotation, pivot = center) {
                drawCircle(
                    color = themeGlow.copy(alpha = 0.5f),
                    radius = baseRadius * 1.15f,
                    center = center,
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 25f), 0f)
                    )
                )
            }

            // 3. Draw Outer Solid Border Ring with ticks
            drawCircle(
                color = themeBright.copy(alpha = 0.7f),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(width = 2f)
            )
            for (i in 0 until 360 step 15) {
                val rad = Math.toRadians(i.toDouble())
                val start = Offset(
                    (center.x + baseRadius * 1.05f * cos(rad)).toFloat(),
                    (center.y + baseRadius * 1.05f * sin(rad)).toFloat()
                )
                val endRadius = if (i % 45 == 0) baseRadius * 1.12f else baseRadius * 1.08f
                val end = Offset(
                    (center.x + endRadius * cos(rad)).toFloat(),
                    (center.y + endRadius * sin(rad)).toFloat()
                )
                drawLine(
                    color = themeBright.copy(alpha = 0.6f),
                    start = start,
                    end = end,
                    strokeWidth = 2f
                )
            }

            // 4. Draw Middle Rotating Ring (Fast clockwise rotation)
            rotate(degrees = innerRotation, pivot = center) {
                // Circular segments representing reactor shards
                drawCircle(
                    color = themeBright,
                    radius = baseRadius * 0.85f,
                    center = center,
                    style = Stroke(
                        width = 8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(80f, 40f), 0f)
                    )
                )
            }

            // 5. Draw Core Reactor Base Glow
            drawCircle(
                color = themeDim.copy(alpha = 0.3f),
                radius = baseRadius * 0.6f * pulseScale,
                center = center
            )

            // 6. Draw Internal Glowing Core (Reactor Center)
            val innerPowerScale = (powerPercent / 100f).coerceIn(0.2f, 1.5f)
            drawCircle(
                color = themeBright.copy(alpha = 0.9f),
                radius = baseRadius * 0.4f * pulseScale * innerPowerScale,
                center = center,
                style = Stroke(width = 6f)
            )

            // Draw center dot
            drawCircle(
                color = Color.White,
                radius = baseRadius * 0.1f * pulseScale,
                center = center
            )
        }
    }
}
