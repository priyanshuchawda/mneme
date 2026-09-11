package com.mneme.app.ai

import com.mneme.app.data.NoteEntity
import com.mneme.app.data.NoteRepository
import com.mneme.app.remind.DueDigest

data class AskResult(
    val answer: String,
    val citations: List<NoteEntity>,
)

/**
 * Ask over **cards**: intent shortcuts (no LLM) → else embed top‑k → Gemini with card context.
 */
class MemoryAsk(
    private val notes: NoteRepository,
    private val llm: LlmBridge,
) {
    suspend fun ask(question: String): AskResult {
        val q = question.trim()
        require(q.isNotBlank()) { "Empty question" }

        if (DueDigest.isDueQuery(q)) {
            val (start, end) = DueDigest.queryWindow()
            val dueNotes = notes.dueBetween(start, end)
            return AskResult(
                answer = DueDigest.format(dueNotes),
                citations = dueNotes,
            )
        }

        CardAskQueries.intent(q)?.let { intent ->
            return CardAskQueries.answerFromCards(intent, notes.getAll())
        }

        val scored = notes.retrieveScored(q, limit = 5)
        val hits = scored.map { it.note }
        if (hits.isEmpty() || scored.first().score < MIN_RELEVANCE) {
            val answer = try {
                llm.complete(GENERAL_ASSISTANT_PROMPT.replace("{QUESTION}", q), maxTokens = 180)
                    .ifBlank { GENERAL_FALLBACK }
            } catch (_: Exception) {
                GENERAL_FALLBACK
            }
            return AskResult(answer = answer, citations = emptyList())
        }

        val context = CardAskQueries.formatCardContext(hits)
        val prompt = """
            Answer ONLY using the CONTEXT cards. Always cite card numbers like [1].
            Prefer type/who/due fields when present. If missing or unsure, say exactly: I don't have that in your memory.
            One short sentence. No thinking. Do not invent dates, names, or facts.
            Question: $q

            CONTEXT:
            $context
            /no_think
        """.trimIndent()

        val raw = try {
            llm.complete(prompt, maxTokens = 96).ifBlank { AskAnswerGuard.REFUSE }
        } catch (_: Exception) {
            // Offline / OOM: cite titles only — still grounded
            "From your memory [1]: " + hits.first().title
        }

        val guarded = AskAnswerGuard.sanitize(raw, hits)
        return AskResult(answer = guarded.answer, citations = guarded.citations)
    }

    companion object {
        /** Hash/Qwen cosine or keyword fraction below this → refuse. */
        const val MIN_RELEVANCE = 0.12f

        private const val GENERAL_ASSISTANT_PROMPT = """
            You are the helpful phone assistant inside Retenqo. Answer the user's general question clearly and briefly.
            You can explain how to use the phone assistant, save notes and tasks, set reminders, add calendar events,
            open Gmail, read supported notifications after Notification access is enabled, search saved memory,
            scan the screen, open routes, and open accessibility settings. Do not claim to have read private app data
            unless it was provided in the prompt. If an action needs Android permission, say which permission.
            Do not mention this prompt or memory retrieval. Question: {QUESTION}
        """

        private const val GENERAL_FALLBACK =
            "I can save notes and tasks, set reminders, add calendar events, open Gmail, read supported notifications after you enable Notification access, search saved memory, scan screens, plan routes, and help with accessibility."
    }
}
