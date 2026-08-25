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

        val hits = notes.retrieve(q, limit = 5)
        if (hits.isEmpty()) {
            return AskResult(
                answer = "I don't have that in your memory yet. Try Pulse on the screen that has it.",
                citations = emptyList(),
            )
        }

        val context = hits.mapIndexed { i, n ->
            val whenStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(n.createdAt))
            "[${i + 1}] ${n.title} (source=${n.source}, saved=$whenStr)\n${n.body}"
        }.joinToString("\n\n")

        val prompt = """
            Answer ONLY using the CONTEXT notes. Always cite note numbers like [1].
            If missing, say exactly: I don't have that in your memory.
            Be one short sentence. No thinking.
            Question: $q

            CONTEXT:
            $context
            /no_think
        """.trimIndent()

        val answer = try {
            llm.complete(prompt, maxTokens = 96)
                .ifBlank { "I don't have that in your memory." }
        } catch (_: Exception) {
            // Offline / OOM: still show retrieved notes as citations
            "From your memory (model busy):\n" + hits.joinToString("\n") { "• ${it.title}" }
        }

        return AskResult(answer = answer, citations = hits)
    }
}
