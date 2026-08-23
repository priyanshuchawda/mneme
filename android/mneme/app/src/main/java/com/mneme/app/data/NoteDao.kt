package com.mneme.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Query(
        """
        SELECT * FROM notes
        WHERE body LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%'
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
