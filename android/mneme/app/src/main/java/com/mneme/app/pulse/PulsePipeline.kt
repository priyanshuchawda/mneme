package com.mneme.app.pulse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.NoteExtractor
import com.mneme.app.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PulseResult(
    val noteId: Long?,
    val preview: String,
    val skippedDuplicate: Boolean,
)

/**
 * Pulse: screenshot → OCR → optional Qwen JSON extract → Room note → recycle pixels.
 */
class PulsePipeline(
    private val context: Context,
    private val notes: NoteRepository,
    private val llm: LlmBridge? = null,
) {
    private val mutex = Mutex()
    private var lastPulseAt = 0L

    suspend fun runFromBitmap(bitmap: Bitmap, sourceLabel: String = "Pulse"): PulseResult =
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastPulseAt < 3_000L) {
                return PulseResult(null, "Pulse ignored (debounce)", skippedDuplicate = false)
            }
            lastPulseAt = now

            try {
                val text = recognize(bitmap).trim()
                if (!shouldSave(text)) {
                    return PulseResult(null, "Nothing important on screen", skippedDuplicate = false)
                }

                val extracted = if (llm != null) {
                    NoteExtractor.extract(llm, text)
                } else {
                    NoteExtractor.fallback(text)
                }

                val id = notes.saveIfNew(
                    title = extracted.title,
                    body = extracted.body,
                    source = sourceLabel,
                    dueAt = extracted.dueAt,
                )
                if (id == null) {
                    PulseResult(null, "Already saved recently", skippedDuplicate = true)
                } else {
                    val dueHint = if (extracted.dueAt != null) " · due set" else ""
                    PulseResult(id, "Saved: ${extracted.title}$dueHint", skippedDuplicate = false)
                }
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

    suspend fun runFromFile(file: File, sourceLabel: String = "Pulse"): PulseResult =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return@withContext PulseResult(null, "Could not read screenshot", false)
            try {
                runFromBitmap(bitmap, sourceLabel)
            } finally {
                if (file.exists()) file.delete()
            }
        }

    private suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    companion object {
        private val SAVE_HINTS = Regex(
            """(?i)(due|deadline|submit|exam|chapter|meeting|homework|reminder|tomorrow|aug|sept|oct|nov|dec|jan|feb|mar|apr|may|jun|jul|\d{1,2}[:/.-]\d{1,2}|@|\.com|http)""",
        )

        fun shouldSave(text: String): Boolean {
            val t = text.trim()
            if (t.length < 10) return false
            val lower = t.lowercase()
            if (lower in setOf("ok", "okay", "thanks", "thank you", "lol", "haha", "yes", "no")) return false
            return SAVE_HINTS.containsMatchIn(t) || t.length >= 40
        }
    }
}
