package com.starkindustries.jarvis.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onTelemetryLog: (String, String) -> Unit,
    private val onSpeechDone: () -> Unit
) {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null
    private var isTtsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    var isFriday = false
        set(value) {
            field = value
            configureTtsVoice()
        }

    init {
        // Initialize TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                configureTtsVoice()
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onTelemetryLog("Vocal playback started.", "info")
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "jarvis_speech_id") {
                            // Run on main thread to update UI and restart listener safely
                            mainHandler.post {
                                onSpeechDone()
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        onTelemetryLog("Vocal synthesizer playback error.", "error")
                    }
                })

                onTelemetryLog("Vocal synthesizer loaded. Language calibrated.", "info")
            } else {
                onTelemetryLog("Vocal synthesizer failure. Offline mode activated.", "error")
            }
        }

        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            setupRecognizerListener()
        } else {
            onTelemetryLog("Speech recognition hardware not detected on this interface.", "error")
        }
    }

    private fun configureTtsVoice() {
        if (!isTtsReady) return
        val locale = if (isFriday) Locale.US else Locale.UK
        tts?.language = locale
        // Configure pitch and speed for appropriate Jarvis/Friday persona
        if (isFriday) {
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.10f)
        } else {
            tts?.setPitch(0.92f)
            tts?.setSpeechRate(1.02f)
        }
    }

    private fun setupRecognizerListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onTelemetryLog("Audio interface open. Listening for command...", "info")
            }

            override fun onBeginningOfSpeech() {
                onTelemetryLog("Vocal input detected.", "info")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onTelemetryLog("Processing vocal input...", "info")
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No command match"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Interface disruption"
                }
                onTelemetryLog("Acoustic calibration error: $message", "error")
                
                // If it was speech timeout or no match, wait a bit and restart listening
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    mainHandler.postDelayed({
                        onSpeechDone()
                    }, 800)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onSpeechResult(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun speak(text: String) {
        if (!isTtsReady) return
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_speech_id")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "jarvis_speech_id")
        onTelemetryLog("Jarvis responds: \"$text\"", "info")
    }

    fun startListening() {
        speechRecognizer?.startListening(speechIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
