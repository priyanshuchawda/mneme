package com.mneme.app.ai

import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class ExtractedNote(
    val title: String,
    val body: String,
    val dueAt: Long?,
)

object NoteExtractor {
    private val duePatterns = listOf(
        Regex("""(?i)\b(due|deadline|by|on)\s+([A-Za-z]+\s+\d{1,2}(?:,?\s*\d{4})?)"""),
        Regex("""(?i)\b(\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)\b"""),
        Regex("""(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\s+\d{1,2}(?:,?\s*\d{4})?\b"""),
    )

    suspend fun extract(llm: LlmBridge, ocrText: String): ExtractedNote {
        val prompt = """
            Extract a personal memory note as JSON only (no markdown).
            Keys: title (short), body (cleaned facts), due_date (YYYY-MM-DD or null).
            Text:
            ${ocrText.take(1200)}
        """.trimIndent()

        return try {
            llm.ensureLoaded()
            val raw = llm.complete(prompt, maxTokens = 120)
            parseJson(raw, ocrText)
        } catch (_: Exception) {
            fallback(ocrText)
        }
    }

    fun fallback(ocrText: String): ExtractedNote {
        val title = ocrText.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Screen note"
        return ExtractedNote(title = title, body = ocrText.trim(), dueAt = guessDue(ocrText))
    }

    private fun parseJson(raw: String, fallbackText: String): ExtractedNote {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return fallback(fallbackText)
        val obj = JSONObject(raw.substring(start, end + 1))
        val title = obj.optString("title").ifBlank {
            fallbackText.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Note"
        }
        val body = obj.optString("body").ifBlank { fallbackText.trim() }
        val due = obj.optString("due_date").takeIf { it.isNotBlank() && it != "null" }
        return ExtractedNote(title = title, body = body, dueAt = parseDue(due) ?: guessDue(fallbackText))
    }

    private fun parseDue(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf("yyyy-MM-dd", "MMM d yyyy", "MMM d, yyyy", "MMMM d yyyy")
        for (f in formats) {
            try {
                val d = LocalDate.parse(value, DateTimeFormatter.ofPattern(f, Locale.ENGLISH))
                return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    private fun guessDue(text: String): Long? {
        for (p in duePatterns) {
            val m = p.find(text) ?: continue
            val chunk = m.groupValues.lastOrNull { it.isNotBlank() } ?: continue
            parseDue(chunk)?.let { return it }
            // try "August 25" + current year
            try {
                val withYear = "$chunk ${LocalDate.now().year}"
                val d = LocalDate.parse(withYear, DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH))
                return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
            try {
                val withYear = "$chunk ${LocalDate.now().year}"
                val d = LocalDate.parse(withYear, DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH))
                return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }
}
