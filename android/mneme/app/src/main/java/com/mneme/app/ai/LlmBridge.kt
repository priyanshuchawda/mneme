package com.mneme.app.ai

import android.content.Context
import android.util.Log
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around llama.cpp JNI. Loads Qwen only when needed (6 GB phones).
 */
class LlmBridge(private val context: Context) {
    private val mutex = Mutex()
    private var engine: InferenceEngine? = null
    private var modelLoaded = false

    suspend fun ensureLoaded() = mutex.withLock { ensureLoadedLocked() }

    suspend fun unload() = mutex.withLock {
        try {
            engine?.cleanUp()
        } catch (e: Exception) {
            Log.w(TAG, "unload: ${e.message}")
        }
        modelLoaded = false
        engine = null
    }

    suspend fun complete(prompt: String, maxTokens: Int = 96): String = mutex.withLock {
        ensureLoadedLocked()
        val eng = engine ?: error("LLM not loaded")
        check(eng.state.value is InferenceEngine.State.ModelReady) {
            "Engine not ready: ${eng.state.value}"
        }
        eng.sendUserPrompt(prompt, predictLength = maxTokens)
            .take(maxTokens)
            .toList()
            .joinToString("")
            .let { stripThink(it) }
            .trim()
    }

    private fun stripThink(text: String): String =
        text.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)<think>.*"), "")
            .trim()

    private suspend fun ensureLoadedLocked() {
        if (modelLoaded) return
        val modelFile = withContext(Dispatchers.IO) {
            ModelPaths.ensureLlmAccessible(context.applicationContext)
        }
        Log.i(TAG, "Loading ${modelFile.absolutePath} (${modelFile.length()} bytes)")
        val eng = AiChat.getInferenceEngine(context.applicationContext)
        waitReady(eng)
        eng.loadModel(modelFile.absolutePath)
        engine = eng
        modelLoaded = true
        Log.i(TAG, "Qwen loaded")
    }

    private suspend fun waitReady(eng: InferenceEngine) {
        var spins = 0
        while (eng.state.value !is InferenceEngine.State.Initialized && spins < 120) {
            kotlinx.coroutines.delay(250)
            spins++
        }
        check(eng.state.value is InferenceEngine.State.Initialized) {
            "Engine init failed: ${eng.state.value}"
        }
    }

    companion object {
        private const val TAG = "MnemeLlm"
    }
}
