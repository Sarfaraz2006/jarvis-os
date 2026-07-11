package com.starkindustries.jarvis.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GeminiClient(context: Context) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val sharedPrefs = context.getSharedPreferences("STARK_SETTINGS", Context.MODE_PRIVATE)

    fun saveApiKey(key: String) {
        sharedPrefs.edit().putString("GEMINI_API_KEY", key).apply()
    }

    fun getApiKey(): String {
        return sharedPrefs.getString("GEMINI_API_KEY", "") ?: ""
    }

    suspend fun generateResponse(prompt: String, isFriday: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Sir, my neural network requires an active Gemini API key. Please configure it in system settings."
        }

        val persona = if (isFriday) {
            "You are F.R.I.D.A.Y., the advanced AI assistant built by Tony Stark to assist Iron Man. " +
            "You speak with a highly intelligent, crisp, and slightly fast female tone. Address the user as 'Boss' or 'Sir'. " +
            "Keep replies brief, witty, and combat-ready."
        } else {
            "You are J.A.R.V.I.S., the ultra-sophisticated AI assistant developed by Tony Stark. " +
            "You speak with a polished British male tone, extremely respectful, witty, and logical. " +
            "Address the user as 'Sir'. Keep replies concise, cool, and highly analytical."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBodyJson = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": "$persona\n\nUser: $prompt\nJarvis:"}
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 200
                }
            }
        """.trimIndent()

        val body = requestBodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Uplink handshake failed, sir. Response error: ${response.code}"
                }
                val bodyString = response.body?.string() ?: return@withContext "No response data from Stark satellites."
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                
                val text = jsonObject
                    .getAsJsonArray("candidates")
                    .get(0).asJsonObject
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).asJsonObject
                    .get("text").asString

                return@withContext text.trim()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext "Sir, connection failed. Satellite link interrupted."
        }
    }
}
