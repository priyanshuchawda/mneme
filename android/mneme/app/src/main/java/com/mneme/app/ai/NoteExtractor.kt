package com.mneme.app.ai

import org.json.JSONArray
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
    /** Text used for embeddings / RAG (cleaner than raw OCR). */
    val embedText: String = body,
    val kind: String = "other",
    val important: Boolean = true,
)

/**
 * Pulse OCR → structured memory card.
 *
 * Fedora eval (eval-results/pulse-store): rich schema B beats dumping OCR into body.
 * We flatten extras into [body] so Room stays title/body/dueAt/embedding.
 */
object NoteExtractor {
    private val duePatterns = listOf(
        Regex("""(?i)\b(due|deadline|by|on)\s+([A-Za-z]+\s+\d{1,2}(?:,?\s*\d{4})?)"""),
        Regex("""(?i)\b(\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)\b"""),
        Regex("""(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\s+\d{1,2}(?:,?\s*\d{4})?\b"""),
    )

    suspend fun extract(llm: LlmBridge, ocrText: String): ExtractedNote {
        val today = LocalDate.now()
        val prompt = """
            Extract ONE personal memory card as JSON only (no markdown, no thinking).
            Keys:
            - title (short, searchable — prefer group/person/topic; NEVER a clock time alone like "18:54")
            - kind (meeting|deadline|receipt|chat|reminder|other)
            - people (array of names)
            - chat_with (person or group name, or null)
            - body (2-4 cleaned sentences: who / what / why — no OCR junk, no UI chrome, no status bar times)
            - when_text (e.g. "9:30" or "Thu 4pm" or null — times without a calendar date)
            - due_date (YYYY-MM-DD only if a real calendar date is present; else null)
            - tags (2-5 keywords)
            - important (true for meet/deadline/money/notice/exam; false for casual chat)
            - embed_text (one clear English paragraph for search: names + facts + times)
            Today is $today. If a date has no year, use ${today.year}.
            Do NOT invent a due_date from clock times like 13:49 or 9:30 alone.
            If the text is mostly an app's own UI (settings, home screen chrome), set important=false.
            Text:
            ${ocrText.take(1400)}
            /no_think
        """.trimIndent()

        return try {
            llm.ensureLoaded()
            val raw = llm.complete(prompt, maxTokens = 220)
            parseJson(raw, ocrText)
        } catch (_: Exception) {
            fallback(ocrText)
        }
    }

    fun fallback(ocrText: String): ExtractedNote {
        val title = ocrText.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.matches(Regex("""^\d{1,2}:\d{2}$""")) }
            ?.take(80)
            ?: "Screen note"
        val body = ocrText.trim()
        return ExtractedNote(
            title = title,
            body = body,
            dueAt = guessDue(ocrText),
            embedText = body.take(800),
            kind = "other",
            important = PulsePipelineHints.looksImportant(ocrText),
        )
    }

    private fun parseJson(raw: String, fallbackText: String): ExtractedNote {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return fallback(fallbackText)
        return try {
            val obj = JSONObject(raw.substring(start, end + 1))
            val title = obj.optString("title").ifBlank {
                fallbackText.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Note"
            }
            val kind = obj.optString("kind").ifBlank { "other" }
            val chatWith = obj.optString("chat_with").takeIf { it.isNotBlank() && it != "null" }
            val people = jsonStringList(obj.opt("people"))
            val whenText = obj.optString("when_text").takeIf { it.isNotBlank() && it != "null" }
            val tags = jsonStringList(obj.opt("tags"))
            val bodyRaw = when (val b = obj.opt("body")) {
                is JSONArray -> (0 until b.length()).joinToString(" ") { b.optString(it) }
                else -> obj.optString("body")
            }.ifBlank { fallbackText.trim() }
            val embedText = obj.optString("embed_text").ifBlank {
                buildEmbedText(title, kind, chatWith, people, bodyRaw, whenText, tags)
            }
            val due = obj.optString("due_date").takeIf { it.isNotBlank() && it != "null" }
            val important = if (obj.has("important")) obj.optBoolean("important", true) else true

            val storedBody = formatStoredBody(
                kind = kind,
                chatWith = chatWith,
                people = people,
                body = bodyRaw,
                whenText = whenText,
                tags = tags,
            )

            ExtractedNote(
                title = title.take(120),
                body = storedBody,
                dueAt = normalizeDueYear(parseDue(due)) ?: guessDue(fallbackText),
                embedText = embedText.take(1200),
                kind = kind,
                important = important,
            )
        } catch (_: Exception) {
            fallback(fallbackText)
        }
    }

    private fun jsonStringList(any: Any?): List<String> = when (any) {
        is JSONArray -> (0 until any.length()).mapNotNull {
            any.optString(it).takeIf { s -> s.isNotBlank() && s != "null" }
        }
        is String -> any.split(',').map { it.trim() }.filter { it.isNotBlank() }
        else -> emptyList()
    }

    private fun formatStoredBody(
        kind: String,
        chatWith: String?,
        people: List<String>,
        body: String,
        whenText: String?,
        tags: List<String>,
    ): String = buildString {
        append('[').append(kind).append(']')
        if (!chatWith.isNullOrBlank()) append(' ').append(chatWith)
        append('\n').append(body.trim())
        if (!whenText.isNullOrBlank()) append("\nWhen: ").append(whenText)
        if (people.isNotEmpty()) append("\nPeople: ").append(people.joinToString(", "))
        if (tags.isNotEmpty()) append("\nTags: ").append(tags.take(5).joinToString(", "))
    }

    private fun buildEmbedText(
        title: String,
        kind: String,
        chatWith: String?,
        people: List<String>,
        body: String,
        whenText: String?,
        tags: List<String>,
    ): String = buildString {
        append(title).append(". ")
        append(kind).append(". ")
        if (!chatWith.isNullOrBlank()) append("Chat: ").append(chatWith).append(". ")
        if (people.isNotEmpty()) append("People: ").append(people.joinToString(", ")).append(". ")
        append(body.trim())
        if (!whenText.isNullOrBlank()) append(" When: ").append(whenText).append('.')
        if (tags.isNotEmpty()) append(" Tags: ").append(tags.take(5).joinToString(", ")).append('.')
    }.take(1200)

    private fun normalizeDueYear(millis: Long?): Long? {
        if (millis == null) return null
        val d = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val now = LocalDate.now()
        if (kotlin.math.abs(d.year - now.year) <= 1) return millis
        val fixed = LocalDate.of(now.year, d.month, d.dayOfMonth.coerceAtMost(d.month.length(now.isLeapYear)))
        return fixed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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

/** Shared importance heuristics (also used by Pulse filter). */
object PulsePipelineHints {
    private val SAVE_HINTS = Regex(
        """(?i)(due|deadline|submit|exam|chapter|meeting|meet|homework|reminder|tomorrow|notice|audition|festival|lab|cie|₹|rs\.?\s*\d|paid|uber|receipt|aug|sept|oct|nov|dec|jan|feb|mar|apr|may|jun|jul|\d{1,2}[:/.-]\d{1,2}|@|\.com|http)""",
    )

    /** Mneme / launcher chrome — never save as a memory. */
    private val SELF_UI = Regex(
        """(?i)(your private memory stays|search memory|ask only from what you saved|hold mic to ask|ask memory|cited notes|answered from)""",
    )

    fun looksLikeSelfUi(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        val hits = SELF_UI.findAll(t).count()
        val lower = t.lowercase()
        val mnemeHits = Regex("""(?i)\bmneme\b""").findAll(t).count()
        return hits >= 1 || (mnemeHits >= 2 && "pulse" in lower)
    }

    fun looksImportant(text: String): Boolean {
        val t = text.trim()
        if (t.length < 10) return false
        if (looksLikeSelfUi(t)) return false
        val lower = t.lowercase()
        if (lower in setOf("ok", "okay", "thanks", "thank you", "lol", "haha", "yes", "no")) return false
        return SAVE_HINTS.containsMatchIn(t) || t.length >= 40
    }
}
