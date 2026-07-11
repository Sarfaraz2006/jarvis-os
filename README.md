# J.A.R.V.I.S. (Stark OS) - Native Android Kotlin App

A native Android application written in **Kotlin** and built using modern **Jetpack Compose** for a futuristic, interactive Iron Man/Stark Industries inspired HUD.

## Features

1. **Futuristic HUD UI (Jetpack Compose)**
   - Interactive, custom Canvas-drawn **Arc Reactor** with rotating ring segments, expanding radar pulse waves, and glowing core indicators.
   - Dynamic theme shifts based on active protocols (Jarvis Blue, Friday Orange, Safety Green, Red Alert).
2. **Audio Synthesis Core**
   - Synthesizes a real-time background sound hum (typically at 55Hz) directly in Kotlin using the low-level `AudioTrack` API.
   - Adjusts frequency and pitch depending on the active state (e.g. rising hum when power is overloaded).
3. **Voice Engine (Speech-to-Text & Text-to-Speech)**
   - Uses Android's native `SpeechRecognizer` to process verbal inputs hands-free.
   - Uses `TextToSpeech` with custom rates and pitch settings tailored to Jarvis (UK Male Daniel voice) or F.R.I.D.A.Y. (US Female voice).
4. **Native Device Hardware Integration**
   - **Flashlight Control**: Turn on/off flashlight using CameraManager.
   - **App Launcher**: Command the assistant to "open [app name]" (e.g. "open WhatsApp", "open settings") to launch applications directly using Package Intents.
   - **Battery Telemetry**: Real-time broadcast monitor for system battery percentage and charging status.
5. **Gemini Satellite Uplink (AI Chat)**
   - Secure configurations for Gemini API key.
   - Implements custom system instructions to ensure replies match the chosen AI personality.

---

## Codebase Architecture

The project has been structured as a standard modern Gradle Android application:

```
jarvis-android/
├── build.gradle.kts (Root settings)
├── settings.gradle.kts (Module management)
├── gradle.properties (Build flags)
├── local.properties (Android SDK configurations)
└── app/
    ├── build.gradle.kts (App dependencies)
    └── src/
        └── main/
            ├── AndroidManifest.xml (Internet, Audio, Camera, App Query permissions)
            ├── res/
            │   ├── values/
            │   │   ├── strings.xml (App name resources)
            │   │   └── themes.xml (Material Fullscreen Dark styles)
            │   └── xml/
            │       ├── backup_rules.xml
            │       └── data_extraction_rules.xml
            └── java/
                └── com/
                    └── starkindustries/
                        └── jarvis/
                            ├── MainActivity.kt (Core orchestration & broadcast receivers)
                            ├── audio/
                            │   ├── AudioHumGenerator.kt (AudioTrack synth hum)
                            │   └── VoiceEngine.kt (TTS & STT engine)
                            ├── data/
                            │   └── GeminiClient.kt (Gemini API bridge via OkHttp)
                            ├── system/
                            │   └── DeviceController.kt (Hardware integration & packages)
                            └── ui/
                                ├── JarvisApp.kt (Main Compose Scaffold)
                                ├── components/
                                │   ├── ArcReactorHud.kt (Custom Canvas HUD & animations)
                                │   ├── DiagnosticsPanel.kt (Power sliders)
                                │   └── TelemetryFeed.kt (Console logging stream)
                                └── theme/
                                    ├── Color.kt (Stark HUD colors)
                                    ├── Theme.kt (Jarvis/Friday styles)
                                    └── Type.kt (Monospace typography)
```

---

## How to Run in Android Studio

1. Open **Android Studio** (Koala or newer recommended).
2. Choose **Open an Existing Project** and navigate to the directory `jarvis-android/`.
3. Allow Gradle to sync.
4. Ensure you have **Android SDK 34** (Upside Down Cake) installed via SDK Manager.
5. Build and install on a physical Android Device or Emulator.
6. Open the app, click the **Settings Gear** icon on the top right, enter your **Gemini API Key**, and click **Calibrate**.
7. Tap the **Activate Voice** button or click on the **Arc Reactor** core to issue voice commands!

### Voice Command Syntax Examples
- *"Flashlight on"* / *"Torch off"*
- *"Open [App name]"* (e.g. *"open YouTube"*, *"open Gmail"*)
- *"Status"* (Reports battery and reactor system levels)
- *"Red Alert"* (Shifts HUD to alert mode, speeds up hum, changes TTS)
- *"Friday"* (Shifts assistant to F.R.I.D.A.Y. persona)
- *"Stand down"* (Returns to nominal blue J.A.R.V.I.S. protocol)
- *"How is the weather in Malibu today?"* (Sends query to Gemini API)
