package com.starkindustries.jarvis.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioHumGenerator {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var humJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var frequency = 55.0
        set(value) {
            field = value
        }

    @Volatile
    var volume = 0.015f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    @Volatile
    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(volume)
        audioTrack?.play()

        humJob = scope.launch {
            val bufferSize = 1024
            val buffer = ShortArray(bufferSize)
            var angle = 0.0

            while (isActive && isPlaying) {
                val currentFreq = frequency
                val increment = 2.0 * Math.PI * currentFreq / sampleRate

                for (i in 0 until bufferSize) {
                    // Triangle or Sine wave synthesis
                    buffer[i] = (sin(angle) * Short.MAX_VALUE).toInt().toShort()
                    angle += increment
                    if (angle >= 2.0 * Math.PI) {
                        angle -= 2.0 * Math.PI
                    }
                }
                audioTrack?.write(buffer, 0, bufferSize)
            }
        }
    }

    fun stop() {
        isPlaying = false
        humJob?.cancel()
        humJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }
}
