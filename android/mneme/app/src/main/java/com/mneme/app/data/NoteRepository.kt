package com.mneme.app.data

import java.security.MessageDigest

class NoteRepository(private val dao: NoteDao) {
    fun observeNotes() = dao.observeAll()

    suspend fun search(query: String) = dao.search(query.trim())

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
        return dao.insert(
            NoteEntity(
                title = title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
                body = body,
                source = source,
                dueAt = dueAt,
                contentHash = hash,
            )
        ).takeIf { it > 0 }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
