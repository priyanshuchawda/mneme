package com.mneme.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mneme.app.ai.PulsePipelineHints
import com.mneme.app.data.IngestSources
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.PulseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MediaScanResult(
    val scanned: Int,
    val saved: Int,
    val skipped: Int,
    val message: String,
)

/**
 * Ingest WhatsApp Images/Documents from on-disk Media folders.
 * Ladder: image/PDF → ML Kit OCR → [PulsePipeline.ingestText] (Qwen extract + embed).
 * VLM fallback is deferred (OCR-first is enough for digital PDFs / clear photos).
 */
class MediaIngestPipeline(
    private val context: Context,
    private val pulse: PulsePipeline,
) {
    private val mutex = Mutex()
    private val prefs by lazy {
        context.getSharedPreferences("mneme_media_ingest", Context.MODE_PRIVATE)
    }

    suspend fun scanWhatsAppRecent(limit: Int = 8): MediaScanResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val candidates = WhatsAppMediaPaths.listRecentCandidates(limit = limit)
            if (candidates.isEmpty()) {
                return@withContext MediaScanResult(
                    0, 0, 0,
                    "No recent WhatsApp media (download/open a photo or PDF in WA first)",
                )
            }
            var saved = 0
            var skipped = 0
            val notes = mutableListOf<String>()
            for (file in candidates) {
                val key = "done:${file.absolutePath}:${file.lastModified()}:${file.length()}"
                if (prefs.getBoolean(key, false)) {
                    skipped++
                    continue
                }
                val result = try {
                    ingestFile(file)
                } catch (e: Exception) {
                    Log.w(TAG, "ingest failed ${file.name}: ${e.message}")
                    PulseResult(null, "Fail ${file.name}: ${e.message}", false)
                }
                if (result.noteId != null) {
                    saved++
                    prefs.edit().putBoolean(key, true).apply()
                    notes.add(result.preview)
                } else {
                    skipped++
                    // Remember weak/unimportant so we don't loop forever
                    if (result.preview.startsWith("Skipped") ||
                        result.preview.startsWith("Nothing") ||
                        result.skippedDuplicate
                    ) {
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
            }
            val msg = when {
                saved > 0 -> "WA media: saved $saved · skipped $skipped"
                else -> "WA media: nothing new ($skipped checked)"
            }
            MediaScanResult(candidates.size, saved, skipped, msg)
        }
    }

    suspend fun ingestFile(file: File): PulseResult {
        val label = IngestSources.waMedia(file.name)
        val ext = file.extension.lowercase()
        val text = when (ext) {
            "pdf" -> ocrPdf(file)
            in setOf("jpg", "jpeg", "png", "webp", "heic") -> ocrImage(file)
            else -> return PulseResult(null, "Unsupported ${file.extension}", false)
        }
        if (text.length < 12) {
            return PulseResult(null, "Skipped weak OCR: ${file.name}", false)
        }
        if (PulsePipelineHints.shouldSkipIngest(text)) {
            return PulseResult(null, "Skipped junk OCR: ${file.name}", false)
        }
        return pulse.ingestText(text, sourceLabel = label)
    }

    private suspend fun ocrImage(file: File): String = withContext(Dispatchers.IO) {
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
            ?: return@withContext ""
        try {
            recognize(bmp)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    private suspend fun ocrPdf(file: File): String = withContext(Dispatchers.IO) {
        val pages = PdfPageRenderer.renderPages(context, file, maxPages = 2)
        try {
            buildString {
                for ((i, page) in pages.withIndex()) {
                    if (i > 0) append("\n\n")
                    append(recognize(page))
                }
            }.trim()
        } finally {
            pages.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    companion object {
        private const val TAG = "MediaIngest"
    }
}
