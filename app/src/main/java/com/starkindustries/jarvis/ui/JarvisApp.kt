package com.starkindustries.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    onApiKeySaved: (String) -> Unit,
    flashlightState: Boolean,
    onFlashlightToggle: (Boolean) -> Unit
) {
    val colors = JarvisTheme.colors
    var showSettings by remember { mutableStateOf(false) }
    var tempKey by remember { mutableStateOf(apiKey) }

    // Sliders state
    var thrusters by remember { mutableStateOf(0.90f) }
    var repulsors by remember { mutableStateOf(0.75f) }
    var shields by remember { mutableStateOf(0.80f) }
    var lifeSupport by remember { mutableStateOf(0.95f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Futuristic HUD Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.dim, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STARK INDUSTRIES OS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SYSTEM: $themeMode MATRIX",
                        color = colors.bright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Battery Telemetry
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CORE CELL ENERGY",
                            color = colors.textSecondary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$batteryPercent% ${if (isCharging) "[CHARGING]" else ""}",
                            color = colors.bright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.border(1.dp, colors.dim, RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config System",
                            tint = colors.bright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Main Work Area (Grid-like layout using Row and Column)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                
                // Left Panel: System Controls & Modes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .weight(1f)
                            .border(1.dp, colors.dim, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "INTERFACE ASSISTANT",
                            color = colors.bright,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        ThemeButton(
                            label = "J.A.R.V.I.S. (UK Male)",
                            active = themeMode == ThemeMode.JARVIS,
                            onClick = { onThemeChange(ThemeMode.JARVIS) },
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ThemeButton(
                            label = "F.R.I.D.A.Y. (US Female)",
                            active = themeMode == ThemeMode.FRIDAY,
                            onClick = { onThemeChange(ThemeMode.FRIDAY) },
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ThemeButton(
                            label = "GREEN SAFETY MODE",
                            active = themeMode == ThemeMode.SAFETY,
                            onClick = { onThemeChange(ThemeMode.SAFETY) },
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .border(1.dp, colors.dim, RoundedCornerShape(110.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        ArcReactorHud(
                            modifier = Modifier.fillMaxSize(),
                            powerPercent = (thrusters * 25 + repulsors * 25 + shields * 25 + lifeSupport * 25).toInt(),
                            onClick = {
                                // Double click reactor triggers flashlight override
                                onFlashlightToggle(!flashlightState)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio control
                    Button(
                        onClick = { if (isListening) onStopListen() else onStartListen() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) colors.bright else colors.dark,
                            contentColor = if (isListening) Color.Black else colors.bright
                        ),
                        modifier = Modifier.border(1.dp, colors.bright, RoundedCornerShape(4.dp)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Vocal Switch"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "LISTENING..." else "ACTIVATE VOICE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Lower Console: Telemetry & Speech logs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rolling logs
                TelemetryFeed(
                    modifier = Modifier.weight(1.2f),
                    logs = logs
                )

                // Dialog Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, colors.dim, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "SPEECH Handshake CONSOLE",
                        color = colors.bright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Vocal Input: ${speechInput.ifEmpty { "None detected." }}",
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AI System: ${assistantReply.ifEmpty { "Standing by, sir." }}",
                        color = colors.bright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 5
                    )
                }
            }
        }

        // 4. API Configurations Popup
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.background),
                    modifier = Modifier
                        .width(320.dp)
                        .border(1.dp, colors.bright, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STARK ENCRYPTION KEY",
                            color = colors.bright,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showSettings = false }) {
                                Text("CANCEL", color = colors.textSecondary, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onApiKeySaved(tempKey)
                                    showSettings = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.bright),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("CALIBRATE", color = Color.Black, fontFamily = FontFamily.Monospace)
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
            containerColor = if (active) color else Color.Transparent,
            contentColor = if (active) Color.Black else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (active) color else color.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
