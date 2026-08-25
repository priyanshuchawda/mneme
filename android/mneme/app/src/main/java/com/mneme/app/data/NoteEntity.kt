package com.mneme.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["contentHash"]),
        Index(value = ["createdAt"]),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val contentHash: String,
    /** L2-normalized float32 little-endian blob (Qwen3-Embedding-0.6B preferred; Hash fallback). */
    val embedding: ByteArray? = null,
)
