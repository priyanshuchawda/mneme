package com.mneme.app.ai

import android.content.Context
import java.io.File

/**
 * Resolves on-device model files. Prefers app-private copies (Android 13+ MediaProvider
 * blocks random /sdcard reads for non-media types like .gguf).
 */
object ModelPaths {
    private const val SHARED_ROOT = "/sdcard/Mneme/models/phone1"

    fun llmGguf(context: Context): File {
        val name = "Qwen3.5-2B-M-TS-Q4_K_M.gguf"
        val private = File(context.getExternalFilesDir("models"), "llm/$name")
        if (private.isFile && private.canRead() && private.length() > 1_000_000L) return private
        val shared = File("$SHARED_ROOT/llm/$name")
        if (shared.isFile && shared.canRead()) return shared
        return private
    }

    fun ensureLlmAccessible(context: Context): File {
        val preferred = llmGguf(context)
        if (preferred.isFile && preferred.canRead() && preferred.length() > 1_000_000L) return preferred

        val name = "Qwen3.5-2B-M-TS-Q4_K_M.gguf"
        val private = File(context.getExternalFilesDir("models"), "llm/$name")
        val shared = File("$SHARED_ROOT/llm/$name")
        require(shared.isFile) {
            "LLM missing. Push to $SHARED_ROOT/llm/$name or app files ${private.absolutePath}"
        }
        private.parentFile?.mkdirs()
        shared.copyTo(private, overwrite = true)
        require(private.isFile && private.canRead()) { "Failed to stage LLM at ${private.absolutePath}" }
        return private
    }
}
