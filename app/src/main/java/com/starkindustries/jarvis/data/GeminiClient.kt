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

    fun saveModel(model: String) {
        sharedPrefs.edit().putString("GEMINI_MODEL", model).apply()
    }

    fun getModel(): String {
        return sharedPrefs.getString("GEMINI_MODEL", "gemini-1.5-flash") ?: "gemini-1.5-flash"
    }

    fun saveApiFormat(format: String) {
        sharedPrefs.edit().putString("API_FORMAT", format).apply()
    }

    fun getApiFormat(): String {
        return sharedPrefs.getString("API_FORMAT", "GEMINI") ?: "GEMINI"
    }

    fun saveBaseUrl(url: String) {
        sharedPrefs.edit().putString("API_BASE_URL", url).apply()
    }

    fun getBaseUrl(): String {
        val format = getApiFormat()
        val defaultUrl = when (format) {
            "OPENAI" -> "https://api.openai.com"
            "OLLAMA" -> "http://10.0.2.2:11434"
            else -> "https://generativelanguage.googleapis.com"
        }
        return sharedPrefs.getString("API_BASE_URL", defaultUrl) ?: defaultUrl
    }

    suspend fun generateResponse(prompt: String, isFriday: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val format = getApiFormat()
        val baseUrl = getBaseUrl().removeSuffix("/")
        val model = getModel()

        if (apiKey.isEmpty() && format != "OLLAMA") {
            return@withContext "Sir, my neural network requires an active API key. Please configure it in system settings."
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

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBuilder = Request.Builder()

        val requestBodyJson = try {
            when (format) {
                "OPENAI" -> {
                    val url = "$baseUrl/v1/chat/completions"
                    requestBuilder.url(url)
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                    
                    val bodyMap = mapOf(
                        "model" to model,
                        "messages" to listOf(
                            mapOf("role" to "system", "content" to persona),
                            mapOf("role" to "user", "content" to prompt)
                        ),
                        "temperature" to 0.7
                    )
                    gson.toJson(bodyMap)
                }
                "OLLAMA" -> {
                    val url = "$baseUrl/api/chat"
                    requestBuilder.url(url)
                    
                    val bodyMap = mapOf(
                        "model" to model,
                        "messages" to listOf(
                            mapOf("role" to "system", "content" to persona),
                            mapOf("role" to "user", "content" to prompt)
                        ),
                        "stream" to false
                    )
                    gson.toJson(bodyMap)
                }
                else -> { // "GEMINI"
                    val url = "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"
                    requestBuilder.url(url)
                    requestBuilder.addHeader("x-goog-api-key", apiKey)
                    
                    val fullPrompt = "$persona\n\nUser: $prompt\nJarvis:"
                    val bodyMap = mapOf(
                        "contents" to listOf(
                            mapOf(
                                "parts" to listOf(
                                    mapOf("text" to fullPrompt)
                                )
                            )
                        ),
                        "generationConfig" to mapOf(
                            "temperature" to 0.7,
                            "maxOutputTokens" to 250
                        )
                    )
                    gson.toJson(bodyMap)
                }
            }
        } catch (e: Exception) {
            return@withContext "Error constructing payload: ${e.localizedMessage}"
        }

        val body = requestBodyJson.toRequestBody(mediaType)
        requestBuilder.post(body)
        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: return@withContext "No response data from Stark satellites."
                if (!response.isSuccessful) {
                    val helpMsg = when (response.code) {
                        403 -> "Forbidden (403). Sir, please verify that your API Key has the Generative Language API enabled, or your region is allowed."
                        404 -> "Not Found (404). Please verify that your endpoint URL and model identifier '$model' are correct."
                        else -> "Uplink handshake failed (HTTP ${response.code})."
                    }
                    return@withContext "$helpMsg Details: $bodyString"
                }

                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                val replyText = when (format) {
                    "OPENAI" -> {
                        jsonObject.getAsJsonArray("choices")
                            ?.get(0)?.asJsonObject
                            ?.getAsJsonObject("message")
                            ?.get("content")?.asString
                            ?: "No response content parsed."
                    }
                    "OLLAMA" -> {
                        jsonObject.getAsJsonObject("message")
                            ?.get("content")?.asString
                            ?: jsonObject.get("response")?.asString // ollama simple format fallback
                            ?: "No response content parsed."
                    }
                    else -> { // GEMINI
                        jsonObject.getAsJsonArray("candidates")
                            ?.get(0)?.asJsonObject
                            ?.getAsJsonObject("content")
                            ?.getAsJsonArray("parts")
                            ?.get(0)?.asJsonObject
                            ?.get("text")?.asString
                            ?: "No response content parsed."
                    }
                }
                return@withContext replyText.trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Sir, connection failed: ${e.localizedMessage}. Satellite link interrupted."
        }
    }
}
