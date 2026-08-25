package com.mneme.app.ai

import com.mneme.app.data.NoteEntity
import com.mneme.app.data.NoteRepository
import java.text.DateFormat
import java.util.Date

data class AskResult(
    val answer: String,
    val citations: List<NoteEntity>,
)

class MemoryAsk(
    private val notes: NoteRepository,
    private val llm: LlmBridge,
) {
    suspend fun ask(question: String): AskResult {
        val q = question.trim()
        require(q.isNotBlank()) { "Empty question" }

        val scored = notes.retrieveScored(q, limit = 5)
        val hits = scored.map { it.note }
        if (hits.isEmpty() || scored.first().score < MIN_RELEVANCE) {
            return AskResult(answer = AskAnswerGuard.REFUSE, citations = emptyList())
        }

        val context = hits.mapIndexed { i, n ->
            val whenStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(n.createdAt))
            "[${i + 1}] ${n.title} (source=${n.source}, saved=$whenStr)\n${n.body}"
        }.joinToString("\n\n")

        val prompt = """
            Answer ONLY using the CONTEXT notes. Always cite note numbers like [1].
            If missing or unsure, say exactly: I don't have that in your memory.
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
    }
}
