package com.mneme.app.ai

import android.content.Context
import com.mneme.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini-backed text generation used by Ask and OCR note extraction.
 * The API key is injected from ignored Gradle local.properties at build time.
 */
class LlmBridge(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val mutex = Mutex()

    suspend fun ensureLoaded() = Unit

    suspend fun unload() = Unit

    suspend fun complete(prompt: String, maxTokens: Int = 96): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY.trim()
            require(apiKey.isNotEmpty()) {
                "Gemini API key missing. Set gemini.api.key in android/mneme/local.properties."
            }
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }
            try {
                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                    .put("generationConfig", JSONObject()
                        .put("maxOutputTokens", maxTokens)
                        .put("temperature", 0.2))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                if (status !in 200..299) throw IllegalStateException("Gemini HTTP $status: ${response.take(400)}")
                val candidates = JSONObject(response).optJSONArray("candidates") ?: return@withContext ""
                val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    ?: return@withContext ""
                buildString {
                    for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty())
                }.trim()
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        private const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
    }
}
