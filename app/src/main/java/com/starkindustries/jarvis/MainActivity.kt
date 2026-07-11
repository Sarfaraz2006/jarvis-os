package com.starkindustries.jarvis

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.starkindustries.jarvis.audio.AudioHumGenerator
import com.starkindustries.jarvis.audio.VoiceEngine
import com.starkindustries.jarvis.data.GeminiClient
import com.starkindustries.jarvis.system.DeviceController
import com.starkindustries.jarvis.ui.JarvisApp
import com.starkindustries.jarvis.ui.components.TelemetryLog
import com.starkindustries.jarvis.ui.theme.JarvisTheme
import com.starkindustries.jarvis.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String
)

class MainActivity : ComponentActivity() {

    // Engine Modules
    private lateinit var audioHum: AudioHumGenerator
    private lateinit var deviceController: DeviceController
    private lateinit var geminiClient: GeminiClient
    private var voiceEngine: VoiceEngine? = null

    // Compose State Holders
    private var activeTheme by mutableStateOf(ThemeMode.JARVIS)
    private var batteryPercent by mutableStateOf(100)
    private var isBatteryCharging by mutableStateOf(false)
    private var speechInputText by mutableStateOf("")
    private var aiReplyText by mutableStateOf("")
    private var isListeningState by mutableStateOf(false)
    private var flashlightOn by mutableStateOf(false)
    
    // Immersive Live Voice States
    private var isVoiceOverlayOpen by mutableStateOf(false)
    private var voiceModeStatus by mutableStateOf("PAUSED") // LISTENING, THINKING, SPEAKING, PAUSED
    private var liveTranscript by mutableStateOf("")

    private val telemetryLogs = mutableStateListOf<TelemetryLog>()
    private val chatMessages = mutableStateListOf<ChatMessage>()

    // Battery Broadcast Receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPercent = (level * 100 / scale.toFloat()).toInt()

                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize core system utilities
        audioHum = AudioHumGenerator()
        deviceController = DeviceController(this)
        geminiClient = GeminiClient(this)

        logTelemetry("System loading. Initializing Stark kernel core...", "info")
        logTelemetry("Calibrating audio hum generator...", "info")

        // Add initial greeting to Chat
        chatMessages.add(
            ChatMessage(
                id = "init_greet",
                sender = "Jarvis",
                text = "Online and ready, sir. Secure satellite uplink active. Standing by for configurations or vocal commands.",
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
        )

        // 2. Request permissions (Record Audio for voice control, Camera for Flashlight)
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            val camGranted = permissions[Manifest.permission.CAMERA] ?: false

            if (micGranted && camGranted) {
                logTelemetry("Vocal & Optical permissions granted. Full HUD active.", "info")
                initVoiceEngine()
            } else {
                logTelemetry("Permissions denied. Satellite linkage only.", "warning")
                Toast.makeText(this, "Permissions needed for voice HUD control.", Toast.LENGTH_LONG).show()
            }
        }

        // Trigger permissions request
        if (hasRequiredPermissions()) {
            initVoiceEngine()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            )
        }

        // Register Battery status receiver
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // 3. Set content layout
        setContent {
            JarvisTheme(mode = activeTheme) {
                JarvisApp(
                    themeMode = activeTheme,
                    onThemeChange = { mode ->
                        activeTheme = mode
                        updateSystemHumFrequency(mode)
                        val assistantName = if (mode == ThemeMode.FRIDAY) "F.R.I.D.A.Y." else "J.A.R.V.I.S."
                        voiceEngine?.isFriday = (mode == ThemeMode.FRIDAY)
                        voiceEngine?.speak("System theme reconfigured to $assistantName core telemetry, sir.")
                        logTelemetry("Theme changed to $assistantName configuration.", "info")
                    },
                    batteryPercent = batteryPercent,
                    isCharging = isBatteryCharging,
                    logs = telemetryLogs,
                    chatMessages = chatMessages,
                    onSendMessage = { text ->
                        processVoiceCommand(text)
                    },
                    speechInput = speechInputText,
                    assistantReply = aiReplyText,
                    isListening = isListeningState,
                    isVoiceOverlayOpen = isVoiceOverlayOpen,
                    voiceModeStatus = voiceModeStatus,
                    liveTranscript = liveTranscript,
                    onStartListen = {
                        isListeningState = true
                        isVoiceOverlayOpen = true
                        voiceModeStatus = "LISTENING"
                        liveTranscript = "Listening..."
                        voiceEngine?.startListening()
                    },
                    onStopListen = {
                        isListeningState = false
                        isVoiceOverlayOpen = false
                        voiceModeStatus = "PAUSED"
                        voiceEngine?.stopListening()
                    },
                    onToggleVoiceMute = {
                        if (voiceModeStatus == "PAUSED") {
                            voiceModeStatus = "LISTENING"
                            liveTranscript = "Listening..."
                            voiceEngine?.startListening()
                        } else {
                            voiceModeStatus = "PAUSED"
                            liveTranscript = "Voice paused."
                            voiceEngine?.stopListening()
                        }
                    },
                    apiKey = geminiClient.getApiKey(),
                    activeModel = geminiClient.getModel(),
                    apiFormat = geminiClient.getApiFormat(),
                    apiBaseUrl = geminiClient.getBaseUrl(),
                    onSettingsSaved = { key, model, format, baseUrl ->
                        geminiClient.saveApiKey(key.trim())
                        geminiClient.saveModel(model.trim())
                        geminiClient.saveApiFormat(format)
                        geminiClient.saveBaseUrl(baseUrl.trim())
                        logTelemetry("Stark uplink calibrated. Model: $model. Format: $format.", "info")
                    },
                    flashlightState = flashlightOn,
                    onFlashlightToggle = { toggleFlashlight(it) }
                )
            }
        }
    }

    private fun initVoiceEngine() {
        voiceEngine = VoiceEngine(
            context = this,
            onSpeechResult = { text ->
                speechInputText = text
                if (isVoiceOverlayOpen) {
                    liveTranscript = text
                    voiceModeStatus = "THINKING"
                }
                processVoiceCommand(text)
            },
            onTelemetryLog = { msg, type ->
                logTelemetry(msg, type)
            },
            onSpeechDone = {
                // Restart listening automatically in voice mode for continuous conversation
                if (isListeningState && isVoiceOverlayOpen && voiceModeStatus != "PAUSED") {
                    voiceModeStatus = "LISTENING"
                    liveTranscript = "Listening..."
                    voiceEngine?.startListening()
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun processVoiceCommand(command: String) {
        if (command.trim().isEmpty()) return
        val query = command.lowercase(Locale.ROOT)
        logTelemetry("Analyzing speech token: \"$command\"", "info")

        // Add user message to chat list
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        chatMessages.add(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = "User",
                text = command,
                timestamp = time
            )
        )

        lifecycleScope.launch {
            if (isVoiceOverlayOpen) {
                voiceModeStatus = "THINKING"
            }
            
            // Local Hardware control shortcuts
            when {
                query.contains("flashlight on") || query.contains("torch on") -> {
                    toggleFlashlight(true)
                    aiReplyText = "Flashlight activated, sir. Illuminating surrounding sectors."
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                query.contains("flashlight off") || query.contains("torch off") -> {
                    toggleFlashlight(false)
                    aiReplyText = "Flashlight deactivated, sir. Optical stealth mode initialized."
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                query.contains("open") -> {
                    val appToOpen = query.substringAfter("open").trim()
                    val launched = deviceController.launchApplication(appToOpen)
                    aiReplyText = if (launched) {
                        "Opening $appToOpen, sir."
                    } else {
                        "Sir, I could not locate an application named $appToOpen."
                    }
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                query.contains("red alert") || query.contains("alert red") -> {
                    activeTheme = ThemeMode.RED_ALERT
                    voiceEngine?.isFriday = false
                    updateSystemHumFrequency(ThemeMode.RED_ALERT)
                    aiReplyText = "Red alert activated. Combat grids energized. Thrusters and repulsors to maximum!"
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                query.contains("nominal") || query.contains("stand down") || query.contains("jarvis") -> {
                    activeTheme = ThemeMode.JARVIS
                    voiceEngine?.isFriday = false
                    updateSystemHumFrequency(ThemeMode.JARVIS)
                    aiReplyText = "Nominal conditions restored, sir. Standing down weapon controls."
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                query.contains("friday mode") || query.contains("friday") -> {
                    activeTheme = ThemeMode.FRIDAY
                    voiceEngine?.isFriday = true
                    updateSystemHumFrequency(ThemeMode.FRIDAY)
                    aiReplyText = "Friday protocol online. System diagnostics ready, boss. What's the play?"
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                query.contains("status") || query.contains("diagnostics") -> {
                    val chargingStr = if (isBatteryCharging) "charging" else "discharging"
                    aiReplyText = "Suit battery is currently at $batteryPercent percent and is $chargingStr. Core temperature is nominal, sir."
                    addAssistantMessage(aiReplyText)
                    updateVoiceOverlaySpeech(aiReplyText)
                    voiceEngine?.speak(aiReplyText)
                }
                else -> {
                    // Send to AI satellite uplink
                    val reply = geminiClient.generateResponse(command, activeTheme == ThemeMode.FRIDAY)
                    aiReplyText = reply
                    addAssistantMessage(reply)
                    updateVoiceOverlaySpeech(reply)
                    voiceEngine?.speak(reply)
                }
            }
        }
    }

    private fun updateVoiceOverlaySpeech(text: String) {
        if (isVoiceOverlayOpen) {
            voiceModeStatus = "SPEAKING"
            liveTranscript = text
        }
    }

    private fun addAssistantMessage(text: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val senderName = if (activeTheme == ThemeMode.FRIDAY) "Friday" else "Jarvis"
        chatMessages.add(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = senderName,
                text = text,
                timestamp = time
            )
        )
    }

    private fun toggleFlashlight(enabled: Boolean) {
        val success = deviceController.setFlashlight(enabled)
        if (success) {
            flashlightOn = enabled
            logTelemetry("Stark Flashlight set to: ${if (enabled) "ON" else "OFF"}", "info")
        } else {
            logTelemetry("Failed to override device Flashlight hardware.", "error")
        }
    }

    private fun updateSystemHumFrequency(mode: ThemeMode) {
        val freq = when (mode) {
            ThemeMode.JARVIS -> 55.0
            ThemeMode.FRIDAY -> 65.0
            ThemeMode.SAFETY -> 45.0
            ThemeMode.RED_ALERT -> 110.0 // Higher frequency for alarm
        }
        audioHum.frequency = freq
    }

    private fun logTelemetry(message: String, type: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        telemetryLogs.add(TelemetryLog(time, message, type))
    }

    override fun onResume() {
        super.onResume()
        audioHum.start()
        updateSystemHumFrequency(activeTheme)
    }

    override fun onPause() {
        super.onPause()
        audioHum.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        voiceEngine?.release()
    }
}
