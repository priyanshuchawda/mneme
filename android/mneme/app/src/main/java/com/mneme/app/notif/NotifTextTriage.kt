package com.mneme.app.notif

import com.mneme.app.ai.PulsePipelineHints

/**
 * Lightweight filter for notification text before any LLM work.
 */
object NotifTextTriage {
    private val WA_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
    )

    private val SYSTEM_NOISE = Regex(
        """(?i)(checking for new messages|backup in progress|whatsapp web is currently|missed (voice|video) call|ongoing (voice|video) call|calling…|ringing…|you may have new (notifications|messages)|tap to (load|download)|this message was deleted|waiting for this message)""",
    )

    private val MEDIA_ONLY = Regex(
        """(?i)^(📷|🎥|🎙|🎵|📄|sticker|gif|contact card|location)$""",
    )

    fun isWhatsApp(packageName: String?): Boolean =
        packageName != null && packageName in WA_PACKAGES

    /**
     * Returns text worth queuing for ingest, or null to ignore.
     */
    fun candidate(title: String?, body: String?): String? {
        val t = title?.trim().orEmpty()
        val b = body?.trim().orEmpty()
        if (b.isEmpty() && t.isEmpty()) return null
        if (SYSTEM_NOISE.containsMatchIn("$t $b")) return null
        if (b.isNotEmpty() && MEDIA_ONLY.containsMatchIn(b)) return null
        val combined = when {
            t.isNotEmpty() && b.isNotEmpty() -> "From $t: $b"
            b.isNotEmpty() -> b
            else -> t
        }
        if (combined.length < 12) return null
        if (!PulsePipelineHints.looksImportant(combined) && combined.length < 48) return null
        if (PulsePipelineHints.looksLikeSelfUi(combined)) return null
        return combined.take(1_200)
    }
}
