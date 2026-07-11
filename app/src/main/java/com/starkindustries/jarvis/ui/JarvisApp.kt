package com.starkindustries.jarvis.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starkindustries.jarvis.ui.components.ArcReactorHud
import com.starkindustries.jarvis.ui.components.DiagnosticsPanel
import com.starkindustries.jarvis.ui.components.TelemetryFeed
import com.starkindustries.jarvis.ui.components.TelemetryLog
import com.starkindustries.jarvis.ui.theme.JarvisTheme
import com.starkindustries.jarvis.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisApp(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    batteryPercent: Int,
    isCharging: Boolean,
    logs: List<TelemetryLog>,
    speechInput: String,
    assistantReply: String,
    isListening: Boolean,
    onStartListen: () -> Unit,
    onStopListen: () -> Unit,
    apiKey: String,
    activeModel: String,
    onSettingsSaved: (String, String) -> Unit,
    flashlightState: Boolean,
    onFlashlightToggle: (Boolean) -> Unit
) {
    val colors = JarvisTheme.colors
    var showSettings by remember { mutableStateOf(false) }
    var tempKey by remember { mutableStateOf(apiKey) }
    var selectedModel by remember { mutableStateOf(activeModel) }

    // Synchronize local states when values change from outside
    LaunchedEffect(apiKey) {
        tempKey = apiKey
    }
    LaunchedEffect(activeModel) {
        selectedModel = activeModel
    }

    // Sliders state
    var thrusters by remember { mutableStateOf(0.90f) }
    var repulsors by remember { mutableStateOf(0.75f) }
    var shields by remember { mutableStateOf(0.80f) }
    var lifeSupport by remember { mutableStateOf(0.95f) }

    // Pulsing animations for dynamic hud lines
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val hudPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse_alpha"
    )

    // Deep space gradient background
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF020408),
            Color(0xFF070C16),
            Color(0xFF0F182E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Sleek Modern HUD Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, colors.dim.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.bright)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STARK HOLOGRAPHIC LINK",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Text(
                        text = "ACTIVE MATRIX: ${themeMode.name} SYSTEM",
                        color = colors.bright.copy(alpha = hudPulseAlpha),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Battery Telemetry & Settings
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = "ARK-CORE CELL STATUS",
                            color = colors.textSecondary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$batteryPercent% ${if (isCharging) "[STABILIZED]" else "[DISCHARGING]"}",
                            color = colors.bright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, colors.bright.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .background(colors.dark.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config System",
                            tint = colors.bright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Main Work Area (Grid-like layout using Row and Column)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                
                // Left Panel: System Controls & Modes
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Diagnostic Sliders
                    DiagnosticsPanel(
                        modifier = Modifier.weight(1.3f),
                        thrusters = thrusters,
                        repulsors = repulsors,
                        shields = shields,
                        lifeSupport = lifeSupport,
                        onThrustersChange = { thrusters = it },
                        onRepulsorsChange = { repulsors = it },
                        onShieldsChange = { shields = it },
                        onLifeSupportChange = { lifeSupport = it }
                    )

                    // Theme selector buttons
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, colors.dim.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "INTELLIGENCE SYNC MODE",
                            color = colors.bright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ThemeButton(
                            label = "J.A.R.V.I.S. (UK Male)",
                            active = themeMode == ThemeMode.JARVIS,
                            onClick = { onThemeChange(ThemeMode.JARVIS) },
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ThemeButton(
                            label = "F.R.I.D.A.Y. (US Female)",
                            active = themeMode == ThemeMode.FRIDAY,
                            onClick = { onThemeChange(ThemeMode.FRIDAY) },
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ThemeButton(
                            label = "SAFETY MATRIX (Green)",
                            active = themeMode == ThemeMode.SAFETY,
                            onClick = { onThemeChange(ThemeMode.SAFETY) },
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ThemeButton(
                            label = "RED ALERT ENGINE",
                            active = themeMode == ThemeMode.RED_ALERT,
                            onClick = { onThemeChange(ThemeMode.RED_ALERT) },
                            color = colors.bright
                        )
                    }
                }

                // Right Panel: Core HUD Arc Reactor Visuals & Listening Triggers
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, colors.dim.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ARK-REACTOR POWER GRID",
                        color = colors.bright.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .border(1.dp, colors.dim.copy(alpha = 0.3f), RoundedCornerShape(100.dp)),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isListening) "[ VOCAL STREAM ACTIVE ]" else "[ VOICE HANDSHAKE STANDBY ]",
                        color = if (isListening) colors.bright else colors.textSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Audio control
                    Button(
                        onClick = { if (isListening) onStopListen() else onStartListen() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) colors.bright else Color.Transparent,
                            contentColor = if (isListening) Color.Black else colors.bright
                        ),
                        modifier = Modifier
                            .border(1.5.dp, colors.bright, RoundedCornerShape(24.dp))
                            .height(48.dp)
                            .width(200.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Vocal Switch",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isListening) "DISCONNECT" else "SYNC VOICE",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Lower Console: Telemetry & Speech logs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Rolling logs
                TelemetryFeed(
                    modifier = Modifier.weight(1.1f),
                    logs = logs
                )

                // Dialog Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, colors.dim.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "NEURAL COGNITIVE STREAM",
                        color = colors.bright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "INPUT: ${speechInput.ifEmpty { "Waiting for telemetry lock..." }}",
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OUTPUT: ${assistantReply.ifEmpty { "Standing by for vocal synchronization, sir." }}",
                        color = colors.bright,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 4
                    )
                }
            }
        }

        // 4. API Configurations Popup with Glassmorphism
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { /* Block clicks through */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1222)),
                    modifier = Modifier
                        .width(340.dp)
                        .border(1.5.dp, colors.bright, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STARK ENCRYPTION KEY",
                            color = colors.bright,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        OutlinedTextField(
                            value = tempKey,
                            onValueChange = { tempKey = it },
                            label = { Text("Gemini API Key", fontFamily = FontFamily.Monospace) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.bright,
                                unfocusedBorderColor = colors.dim,
                                focusedLabelColor = colors.bright,
                                unfocusedLabelColor = colors.textSecondary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "SELECT QUANTUM MODEL",
                            color = colors.bright,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val models = listOf(
                            "gemini-1.5-flash",
                            "gemini-1.5-pro",
                            "gemini-2.0-flash",
                            "gemini-2.5-flash"
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            models.chunked(2).forEach { rowModels ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowModels.forEach { modelName ->
                                        val isSelected = selectedModel == modelName
                                        Button(
                                            onClick = { selectedModel = modelName },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) colors.bright else Color.Transparent,
                                                contentColor = if (isSelected) Color.Black else Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) colors.bright else colors.dim,
                                                    RoundedCornerShape(6.dp)
                                                ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = modelName.replace("gemini-", ""),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { showSettings = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCEL", color = colors.textSecondary, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    onSettingsSaved(tempKey, selectedModel)
                                    showSettings = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.bright),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CALIBRATE", color = Color.Black, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeButton(
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
