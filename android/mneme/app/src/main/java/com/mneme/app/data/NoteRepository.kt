package com.mneme.app.data

import com.mneme.app.ai.HashEmbedder
import com.mneme.app.ai.MnemeEmbedder

data class ScoredNote(
    val note: NoteEntity,
    val score: Float,
)

class NoteRepository(
    private val dao: NoteDao,
    private val embedder: MnemeEmbedder? = null,
) {
    fun observeNotes() = dao.observeAll()

    suspend fun getAll() = dao.getAll()

    suspend fun search(query: String) = dao.search(query.trim())

    suspend fun retrieve(query: String, limit: Int = 5): List<NoteEntity> =
        retrieveScored(query, limit).map { it.note }

    /**
     * Semantic + keyword retrieval with scores for Ask relevance gating.
     * Prefers active (open / unsnoozed) cards.
     */
    suspend fun retrieveScored(query: String, limit: Int = 5): List<ScoredNote> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val qVec = embed(q)
        val scored = dao.getAll().mapNotNull { note ->
            val vec = fromBytes(note.embedding)
            var score = if (vec != null && vec.size == qVec.size) {
                cosine(qVec, vec)
            } else {
                keywordScore(q, note)
            }
            if (!note.isActive(now)) score *= 0.35f
            if (score > 0.05f) ScoredNote(note, score) else null
        }.sortedByDescending { it.score }

        if (scored.isNotEmpty()) return scored.take(limit)

        return dao.search(q).ifEmpty {
            q.split(Regex("\\s+"))
                .filter { it.length > 2 }
                .flatMap { dao.search(it) }
                .distinctBy { it.id }
        }.take(limit).map { ScoredNote(it, 0.2f) }
    }

    /**
     * Saves a card unless the same content hash was saved in the last 10 minutes.
     * @param embedText preferred text for the vector (cleaner than raw OCR body)
     * @return inserted id, or null if duplicate skipped
     */
    suspend fun saveIfNew(
        title: String,
        body: String,
        source: String,
        dueAt: Long? = null,
        embedText: String? = null,
        who: String? = null,
        cardType: CardType? = null,
        confidence: Float = 1f,
    ): Long? {
        val forEmbed = (embedText?.takeIf { it.isNotBlank() } ?: "$title\n$body")
        val vec = embed(forEmbed)
        val type = cardType ?: CardClassifier.classify(title, body, dueAt, who)
        val hash = IngestSources.contentFingerprint(
            title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
            body,
        )
        val since = System.currentTimeMillis() - 10 * 60_000L
        if (dao.countRecentHash(hash, since) > 0) return null
        return dao.insert(
            NoteEntity(
                title = title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
                body = body,
                source = source,
                dueAt = dueAt,
                contentHash = hash,
                embedding = toBytes(vec),
                cardType = type.storage,
                cardStatus = CardStatus.OPEN.storage,
                who = who?.takeIf { it.isNotBlank() },
                confidence = confidence.coerceIn(0f, 1f),
            )
        ).takeIf { it > 0 }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun markDone(id: Long) {
        dao.updateStatus(
            id = id,
            status = CardStatus.DONE.storage,
            changedAt = System.currentTimeMillis(),
            snoozedUntil = null,
        )
    }

    suspend fun markOpen(id: Long) {
        dao.updateStatus(
            id = id,
            status = CardStatus.OPEN.storage,
            changedAt = System.currentTimeMillis(),
            snoozedUntil = null,
        )
    }

    /** Snooze until [untilMs]; card stays out of active dues until then. */
    suspend fun snooze(id: Long, untilMs: Long) {
        dao.updateStatus(
            id = id,
            status = CardStatus.SNOOZED.storage,
            changedAt = System.currentTimeMillis(),
            snoozedUntil = untilMs,
        )
    }

    suspend fun dueBetween(startMs: Long, endMs: Long): List<NoteEntity> =
        dao.dueBetween(startMs, endMs, System.currentTimeMillis())

    private suspend fun embed(text: String): FloatArray =
        embedder?.embed(text) ?: HashEmbedder.embed(text)

    private fun toBytes(vec: FloatArray): ByteArray =
        embedder?.toBytes(vec) ?: HashEmbedder.toBytes(vec)

    private fun fromBytes(bytes: ByteArray?): FloatArray? =
        embedder?.fromBytes(bytes) ?: HashEmbedder.fromBytes(bytes)

    private fun cosine(a: FloatArray, b: FloatArray): Float =
        embedder?.cosine(a, b) ?: HashEmbedder.cosine(a, b)

    private fun keywordScore(q: String, note: NoteEntity): Float {
        val hay = "${note.title} ${note.body} ${note.who.orEmpty()}".lowercase()
        val tokens = q.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        if (tokens.isEmpty()) return if (hay.contains(q.lowercase())) 0.4f else 0f
        val hits = tokens.count { hay.contains(it) }
        return hits.toFloat() / tokens.size
    }
}
