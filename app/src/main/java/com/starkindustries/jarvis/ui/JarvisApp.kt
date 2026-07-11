package com.starkindustries.jarvis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starkindustries.jarvis.ChatMessage
import com.starkindustries.jarvis.ui.components.TelemetrySidebar
import com.starkindustries.jarvis.ui.components.TelemetryLog
import com.starkindustries.jarvis.ui.theme.JarvisTheme
import com.starkindustries.jarvis.ui.theme.ThemeMode
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisApp(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    batteryPercent: Int,
    isCharging: Boolean,
    logs: List<TelemetryLog>,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    speechInput: String,
    assistantReply: String,
    isListening: Boolean,
    onStartListen: () -> Unit,
    onStopListen: () -> Unit,
    apiKey: String,
    activeModel: String,
    apiFormat: String,
    apiBaseUrl: String,
    onSettingsSaved: (String, String, String, String) -> Unit, // key, model, format, baseUrl
    flashlightState: Boolean,
    onFlashlightToggle: (Boolean) -> Unit
) {
    val colors = JarvisTheme.colors
    var showSettings by remember { mutableStateOf(false) }
    
    var tempKey by remember { mutableStateOf(apiKey) }
    var tempModel by remember { mutableStateOf(activeModel) }
    var tempFormat by remember { mutableStateOf(apiFormat) }
    var tempBaseUrl by remember { mutableStateOf(apiBaseUrl) }

    var textInput by remember { mutableStateOf("") }
    var showTelemetryDrawer by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Sync state
    LaunchedEffect(apiKey) { tempKey = apiKey }
    LaunchedEffect(activeModel) { tempModel = activeModel }
    LaunchedEffect(apiFormat) { tempFormat = apiFormat }
    LaunchedEffect(apiBaseUrl) { tempBaseUrl = apiBaseUrl }

    // Diagnostics sliders state
    var thrusters by remember { mutableStateOf(0.90f) }
    var repulsors by remember { mutableStateOf(0.75f) }
    var shields by remember { mutableStateOf(0.80f) }
    var lifeSupport by remember { mutableStateOf(0.95f) }

    // Pulsing animation for visualizer wave offsets
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF030712),
            Color(0xFF090F1C),
            Color(0xFF111827)
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        val useSplitScreen = maxWidth > 650.dp

        Row(modifier = Modifier.fillMaxSize()) {
            
            // ================= CHAT COMPONENT (LEFT / FULL SCREEN) =================
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Header Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, colors.dim.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.bright)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (themeMode == ThemeMode.FRIDAY) "F.R.I.D.A.Y. SYSTEM" else "J.A.R.V.I.S. INTERFACE",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        
                        Text(
                            text = "FORMAT: $apiFormat  |  MODEL: ${activeModel.uppercase()}",
                            color = colors.bright.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick switch configurations dropdown representation
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.bright.copy(alpha = 0.1f))
                                .border(1.dp, colors.bright.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .clickable { showSettings = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = activeModel.replace("gemini-", "").uppercase(),
                                color = colors.bright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (!useSplitScreen) {
                            IconButton(
                                onClick = { showTelemetryDrawer = !showTelemetryDrawer },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "Diagnostics Dashboard",
                                    tint = colors.bright
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Calibrate Engine",
                                tint = colors.bright
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Messages Feed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.01f))
                        .border(1.dp, colors.dim.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "System link established. Input vocal or text command...",
                                color = colors.textSecondary.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isUser = msg.sender == "User"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) colors.bright.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.02f)
                                        ),
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .border(
                                                1.dp,
                                                if (isUser) colors.bright.copy(alpha = 0.4f) else colors.dim.copy(alpha = 0.2f),
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isUser) 12.dp else 2.dp,
                                                    bottomEnd = if (isUser) 2.dp else 12.dp
                                                )
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = msg.sender.uppercase(),
                                                color = if (isUser) colors.bright else colors.textSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Text(
                                                text = msg.text,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.SansSerif,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Voice Visualizer Waves (Google Assistant / Siri Style)
                if (isListening || assistantReply.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerY = size.height / 2
                            val width = size.width
                            val wavePath = Path()
                            val numWaves = 3

                            for (w in 0 until numWaves) {
                                val waveAlpha = when (w) {
                                    0 -> 0.8f
                                    1 -> 0.5f
                                    else -> 0.3f
                                }
                                val waveColor = colors.bright.copy(alpha = waveAlpha)
                                val amplitude = if (isListening) (10f + w * 4f) else 4f
                                val frequency = 0.015f + w * 0.005f

                                wavePath.reset()
                                wavePath.moveTo(0f, centerY)
                                for (x in 0..width.toInt() step 5) {
                                    val y = centerY + amplitude * sin(x * frequency + waveOffset + w)
                                    wavePath.lineTo(x.toFloat(), y)
                                }
                                drawPath(
                                    path = wavePath,
                                    color = waveColor,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Modern Input Field & Control pill bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Voice Mic Pill Button
                    IconButton(
                        onClick = { if (isListening) onStopListen() else onStartListen() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isListening) colors.bright else Color.White.copy(alpha = 0.03f))
                            .border(
                                1.dp,
                                if (isListening) colors.bright else colors.bright.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Input Control",
                            tint = if (isListening) Color.Black else colors.bright
                        )
                    }

                    // Text Input Bar
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "Ask Jarvis / Friday...",
                                color = colors.textSecondary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.bright,
                            unfocusedBorderColor = colors.dim.copy(alpha = 0.5f),
                            focusedContainerColor = Color.White.copy(alpha = 0.01f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.01f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.trim().isNotEmpty()) {
                                onSendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        }),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (textInput.trim().isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onSendMessage(textInput)
                                        textInput = ""
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = colors.bright
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // ================= STARK TELEMETRY BAR (RIGHT COLUMN FOR TABLETS) =================
            if (useSplitScreen) {
                TelemetrySidebar(
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    batteryPercent = batteryPercent,
                    isCharging = isCharging,
                    logs = logs,
                    thrusters = thrusters,
                    repulsors = repulsors,
                    shields = shields,
                    lifeSupport = lifeSupport,
                    onThrustersChange = { thrusters = it },
                    onRepulsorsChange = { repulsors = it },
                    onShieldsChange = { shields = it },
                    onLifeSupportChange = { lifeSupport = it },
                    flashlightState = flashlightState,
                    onFlashlightToggle = onFlashlightToggle,
                    colors = colors,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                )
            }
        }

        // ================= STARK TELEMETRY PANEL (DRAWER FOR PHONES) =================
        if (!useSplitScreen) {
            AnimatedVisibility(
                visible = showTelemetryDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showTelemetryDrawer = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(300.dp)
                            .background(Color(0xFF070C18))
                            .border(1.dp, colors.bright.copy(alpha = 0.2f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                            .align(Alignment.CenterEnd)
                            .clickable { /* prevent clicks close */ }
                            .padding(16.dp)
                    ) {
                        TelemetrySidebar(
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            batteryPercent = batteryPercent,
                            isCharging = isCharging,
                            logs = logs,
                            thrusters = thrusters,
                            repulsors = repulsors,
                            shields = shields,
                            lifeSupport = lifeSupport,
                            onThrustersChange = { thrusters = it },
                            onRepulsorsChange = { repulsors = it },
                            onShieldsChange = { shields = it },
                            onLifeSupportChange = { lifeSupport = it },
                            flashlightState = flashlightState,
                            onFlashlightToggle = onFlashlightToggle,
                            colors = colors,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // ================= QUANTUM CONFIGURATIONS POPUP =================
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { /* Block clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1224)),
                    modifier = Modifier
                        .width(350.dp)
                        .border(1.5.dp, colors.bright, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "CALIBRATE STARK LINK",
                                color = colors.bright,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp
                            )
                        }

                        // API Format selection row
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "API TYPE",
                                    color = colors.bright,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val formats = listOf("GEMINI", "OPENAI", "OLLAMA")
                                    formats.forEach { fmt ->
                                        val isSelected = tempFormat == fmt
                                        Button(
                                            onClick = {
                                                tempFormat = fmt
                                                // Prepopulate defaults base urls
                                                tempBaseUrl = when (fmt) {
                                                    "OPENAI" -> "https://api.openai.com"
                                                    "OLLAMA" -> "http://10.0.2.2:11434"
                                                    else -> "https://generativelanguage.googleapis.com"
                                                }
                                                // Update default model placeholders
                                                tempModel = when (fmt) {
                                                    "OPENAI" -> "gpt-4o"
                                                    "OLLAMA" -> "llama3"
                                                    else -> "gemini-1.5-flash"
                                                }
                                            },
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
                                                text = fmt,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // API Base URL Field
                        item {
                            OutlinedTextField(
                                value = tempBaseUrl,
                                onValueChange = { tempBaseUrl = it },
                                label = { Text("API Base URL", fontFamily = FontFamily.Monospace) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.bright,
                                    unfocusedBorderColor = colors.dim,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // API Key Input
                        item {
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                label = { Text("Access Key / Token", fontFamily = FontFamily.Monospace) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.bright,
                                    unfocusedBorderColor = colors.dim,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Custom Model Input
                        item {
                            OutlinedTextField(
                                value = tempModel,
                                onValueChange = { tempModel = it },
                                label = { Text("Model Identifier (e.g. gemini-2.5-flash)", fontFamily = FontFamily.Monospace) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.bright,
                                    unfocusedBorderColor = colors.dim,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Save & Cancel Buttons
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
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
                                        onSettingsSaved(tempKey, tempModel, tempFormat, tempBaseUrl)
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
}
