package com.mneme.app.media

import java.io.File

/**
 * WhatsApp saves received/opened media under Android/media (readable with all-files access).
 */
object WhatsAppMediaPaths {
    private const val ROOT = "/sdcard/Android/media/com.whatsapp/WhatsApp/Media"

    val imagesDir: File get() = File("$ROOT/WhatsApp Images")
    val documentsDir: File get() = File("$ROOT/WhatsApp Documents")

    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "heic")
    private val DOC_EXT = setOf("pdf")

    fun listRecentCandidates(maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000, limit: Int = 12): List<File> {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        val files = mutableListOf<File>()
        collect(imagesDir, IMAGE_EXT, cutoff, files)
        collect(documentsDir, DOC_EXT, cutoff, files)
        // Also Sent/ subfolders
        collect(File(imagesDir, "Sent"), IMAGE_EXT, cutoff, files)
        collect(File(documentsDir, "Sent"), DOC_EXT, cutoff, files)
        return files
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
            .take(limit)
    }

    private fun collect(dir: File, exts: Set<String>, cutoff: Long, out: MutableList<File>) {
        if (!dir.isDirectory) return
        dir.listFiles()?.forEach { f ->
            if (!f.isFile) return@forEach
            val ext = f.extension.lowercase()
            if (ext !in exts) return@forEach
            if (f.lastModified() < cutoff) return@forEach
            if (f.length() < 2_000L) return@forEach
            // Skip stickers / tiny thumbs
            if (f.length() > 40L * 1024 * 1024) return@forEach
            out.add(f)
        }
    }
}
