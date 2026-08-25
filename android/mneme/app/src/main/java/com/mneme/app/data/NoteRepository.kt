package com.mneme.app.data

import com.mneme.app.ai.HashEmbedder
import com.mneme.app.ai.MnemeEmbedder
import java.security.MessageDigest

data class ScoredNote(
    val note: NoteEntity,
    val score: Float,
)

class NoteRepository(
    private val dao: NoteDao,
    private val embedder: MnemeEmbedder? = null,
) {
    fun observeNotes() = dao.observeAll()

    suspend fun search(query: String) = dao.search(query.trim())

    suspend fun retrieve(query: String, limit: Int = 5): List<NoteEntity> =
        retrieveScored(query, limit).map { it.note }

    /**
     * Semantic + keyword retrieval with scores for Ask relevance gating.
     */
    suspend fun retrieveScored(query: String, limit: Int = 5): List<ScoredNote> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val qVec = embed(q)
        val scored = dao.getAll().mapNotNull { note ->
            val vec = fromBytes(note.embedding)
            val score = if (vec != null && vec.size == qVec.size) {
                cosine(qVec, vec)
            } else {
                keywordScore(q, note)
            }
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
     * Saves a note unless the same content hash was saved in the last 10 minutes.
     * @param embedText preferred text for the vector (cleaner than raw OCR body)
     * @return inserted id, or null if duplicate skipped
     */
    suspend fun saveIfNew(
        title: String,
        body: String,
        source: String,
        dueAt: Long? = null,
        embedText: String? = null,
    ): Long? {
        val hash = sha256("$title|$body|$source")
        val since = System.currentTimeMillis() - 10 * 60_000L
        if (dao.countRecentHash(hash, since) > 0) return null
        val forEmbed = (embedText?.takeIf { it.isNotBlank() } ?: "$title\n$body")
        val vec = embed(forEmbed)
        return dao.insert(
            NoteEntity(
                title = title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
                body = body,
                source = source,
                dueAt = dueAt,
                contentHash = hash,
                embedding = toBytes(vec),
            )
        ).takeIf { it > 0 }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun dueBetween(startMs: Long, endMs: Long): List<NoteEntity> =
        dao.dueBetween(startMs, endMs)

    private suspend fun embed(text: String): FloatArray =
        embedder?.embed(text) ?: HashEmbedder.embed(text)

    private fun toBytes(vec: FloatArray): ByteArray =
        embedder?.toBytes(vec) ?: HashEmbedder.toBytes(vec)

    private fun fromBytes(bytes: ByteArray?): FloatArray? =
        embedder?.fromBytes(bytes) ?: HashEmbedder.fromBytes(bytes)

    private fun cosine(a: FloatArray, b: FloatArray): Float =
        embedder?.cosine(a, b) ?: HashEmbedder.cosine(a, b)

    private fun keywordScore(q: String, note: NoteEntity): Float {
        val hay = "${note.title} ${note.body}".lowercase()
        val tokens = q.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        if (tokens.isEmpty()) return if (hay.contains(q.lowercase())) 0.4f else 0f
        val hits = tokens.count { hay.contains(it) }
        return hits.toFloat() / tokens.size
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
