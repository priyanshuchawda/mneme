package com.mneme.app.data

import com.mneme.app.ai.HashEmbedder
import java.security.MessageDigest

class NoteRepository(private val dao: NoteDao) {
    fun observeNotes() = dao.observeAll()

    suspend fun search(query: String) = dao.search(query.trim())

    /**
     * Semantic + keyword retrieval. Prefers cosine over stored embeddings; falls back to LIKE.
     */
    suspend fun retrieve(query: String, limit: Int = 5): List<NoteEntity> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val qVec = HashEmbedder.embed(q)
        val scored = dao.getAll().mapNotNull { note ->
            val vec = HashEmbedder.fromBytes(note.embedding)
            val score = if (vec != null) {
                HashEmbedder.cosine(qVec, vec)
            } else {
                keywordScore(q, note)
            }
            if (score > 0.05f) note to score else null
        }.sortedByDescending { it.second }

        if (scored.isNotEmpty()) return scored.take(limit).map { it.first }

        return dao.search(q).ifEmpty {
            q.split(Regex("\\s+"))
                .filter { it.length > 2 }
                .flatMap { dao.search(it) }
                .distinctBy { it.id }
        }.take(limit)
    }

    /**
     * Saves a note unless the same content hash was saved in the last 10 minutes.
     * @return inserted id, or null if duplicate skipped
     */
    suspend fun saveIfNew(
        title: String,
        body: String,
        source: String,
        dueAt: Long? = null,
    ): Long? {
        val hash = sha256("$title|$body|$source")
        val since = System.currentTimeMillis() - 10 * 60_000L
        if (dao.countRecentHash(hash, since) > 0) return null
        val text = "$title\n$body"
        return dao.insert(
            NoteEntity(
                title = title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
                body = body,
                source = source,
                dueAt = dueAt,
                contentHash = hash,
                embedding = HashEmbedder.toBytes(HashEmbedder.embed(text)),
            )
        ).takeIf { it > 0 }
    }

    suspend fun delete(id: Long) = dao.delete(id)

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
