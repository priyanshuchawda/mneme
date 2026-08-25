package com.mneme.app.ai

import android.content.Context
import java.io.File

/**
 * Resolves on-device model files. Prefers app-private copies (Android 13+ MediaProvider
 * blocks random /sdcard reads for non-media types like .gguf / .bin).
 */
object ModelPaths {
    /** Prefer new brand path; keep legacy Mneme path so existing phone models still resolve. */
    private val SHARED_ROOTS = listOf(
        "/sdcard/Retenqo/models/phone1",
        "/sdcard/Mneme/models/phone1",
    )

    fun llmGguf(context: Context): File =
        resolve(context, "llm", "Qwen3.5-2B-M-TS-Q4_K_M.gguf")

    fun whisperBin(context: Context): File =
        resolve(context, "whisper", "ggml-base-q5_1.bin")

    fun embedGguf(context: Context): File =
        resolve(context, "embed/qwen3", "Qwen3-Embedding-0.6B-Q8_0.gguf")

    fun ensureLlmAccessible(context: Context): File =
        ensureAccessible(context, "llm", "Qwen3.5-2B-M-TS-Q4_K_M.gguf", minBytes = 1_000_000L)

    fun ensureWhisperAccessible(context: Context): File =
        ensureAccessible(context, "whisper", "ggml-base-q5_1.bin", minBytes = 1_000_000L)

    fun ensureEmbedAccessible(context: Context): File =
        ensureAccessible(
            context,
            "embed/qwen3",
            "Qwen3-Embedding-0.6B-Q8_0.gguf",
            minBytes = 1_000_000L,
        )

    private fun resolve(context: Context, subdir: String, name: String): File {
        val private = File(context.getExternalFilesDir("models"), "$subdir/$name")
        if (private.isFile && private.canRead() && private.length() > 1_000_000L) return private
        for (root in SHARED_ROOTS) {
            val shared = File("$root/$subdir/$name")
            if (shared.isFile && shared.canRead()) return shared
        }
        return private
    }

    private fun ensureAccessible(
        context: Context,
        subdir: String,
        name: String,
        minBytes: Long,
    ): File {
        val preferred = resolve(context, subdir, name)
        if (preferred.isFile && preferred.canRead() && preferred.length() > minBytes) return preferred

        val private = File(context.getExternalFilesDir("models"), "$subdir/$name")
        val shared = SHARED_ROOTS
            .map { File("$it/$subdir/$name") }
            .firstOrNull { it.isFile }
        require(shared != null) {
            "Model missing. Push to ${SHARED_ROOTS[0]}/$subdir/$name (or legacy Mneme path) or ${private.absolutePath}"
        }
        private.parentFile?.mkdirs()
        shared.copyTo(private, overwrite = true)
        require(private.isFile && private.canRead()) { "Failed to stage model at ${private.absolutePath}" }
        return private
    }
}
