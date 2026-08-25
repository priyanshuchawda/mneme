package com.mneme.app.ai

import android.content.Context
import android.util.Log
import com.arm.aichat.EmbeddingEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Prefers Qwen3-Embedding-0.6B (proven better RAG than Hash). Falls back to [HashEmbedder]
 * if the GGUF is missing or native load fails.
 */
class MnemeEmbedder(
    private val context: Context,
    private val llm: LlmBridge? = null,
) {
    private val engine = EmbeddingEngine.get(context.applicationContext)
    private val mutex = Mutex()
    @Volatile private var qwenReady: Boolean? = null

    suspend fun embed(text: String): FloatArray = mutex.withLock {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withLock HashEmbedder.embed("")

        if (tryQwen()) {
            try {
                // Free chat LLM RAM before loading 610 MB embed GGUF on Phone 1.
                llm?.unload()
                val model = ModelPaths.ensureEmbedAccessible(context.applicationContext)
                check(engine.load(model.absolutePath)) { "Qwen embed load failed" }
                val vec = engine.embed(trimmed)
                if (vec != null && vec.isNotEmpty()) {
                    Log.d(TAG, "Qwen embed dim=${vec.size}")
                    return@withLock vec
                }
            } catch (e: Exception) {
                Log.w(TAG, "Qwen embed failed, using Hash: ${e.message}")
                qwenReady = false
            } finally {
                try {
                    engine.unload()
                } catch (_: Exception) {
                }
            }
        }
        HashEmbedder.embed(trimmed)
    }

    fun toBytes(vec: FloatArray): ByteArray = HashEmbedder.toBytes(vec)
    fun fromBytes(bytes: ByteArray?): FloatArray? = HashEmbedder.fromBytes(bytes)
    fun cosine(a: FloatArray, b: FloatArray): Float = HashEmbedder.cosine(a, b)

    private fun tryQwen(): Boolean {
        qwenReady?.let { return it }
        return try {
            val f = ModelPaths.embedGguf(context.applicationContext)
            val ok = f.isFile && f.length() > 1_000_000L
            qwenReady = ok
            if (!ok) Log.i(TAG, "Qwen embed GGUF not found yet → HashEmbedder")
            ok
        } catch (e: Exception) {
            qwenReady = false
            false
        }
    }

    companion object {
        private const val TAG = "MnemeEmbedder"
    }
}
