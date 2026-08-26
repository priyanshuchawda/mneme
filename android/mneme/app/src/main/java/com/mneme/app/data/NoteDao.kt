package com.mneme.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        ORDER BY
          CASE cardStatus
            WHEN 'open' THEN 0
            WHEN 'snoozed' THEN 1
            WHEN 'done' THEN 2
            ELSE 3
          END,
          createdAt DESC
        """
    )
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Query(
        """
        SELECT * FROM notes
        WHERE body LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%' OR IFNULL(who, '') LIKE '%' || :q || '%'
        ORDER BY createdAt DESC
        LIMIT 50
        """
    )
    suspend fun search(q: String): List<NoteEntity>

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE contentHash = :hash AND createdAt > :since
        """
    )
    suspend fun countRecentHash(hash: String, since: Long): Int

    @Query(
        """
        SELECT * FROM notes
        WHERE dueAt IS NOT NULL
          AND dueAt >= :startMs AND dueAt <= :endMs
          AND cardStatus IN ('open', 'snoozed')
          AND (cardStatus = 'open' OR snoozedUntil IS NULL OR snoozedUntil <= :nowMs)
        ORDER BY dueAt ASC
        """
    )
    suspend fun dueBetween(startMs: Long, endMs: Long, nowMs: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        UPDATE notes SET
          cardStatus = :status,
          statusChangedAt = :changedAt,
          snoozedUntil = :snoozedUntil
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: Long,
        status: String,
        changedAt: Long,
        snoozedUntil: Long?,
    )

    @Query(
        """
        UPDATE notes SET
          title = :title,
          body = :body,
          who = :who,
          dueAt = :dueAt,
          cardType = :cardType
        WHERE id = :id
        """
    )
    suspend fun updateCardFields(
        id: Long,
        title: String,
        body: String,
        who: String?,
        dueAt: Long?,
        cardType: String,
    )
}
