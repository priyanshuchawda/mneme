package com.mneme.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class], version = 3, exportSchema = false)
abstract class MnemeDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao

    companion object {
        @Volatile private var instance: MnemeDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN cardType TEXT NOT NULL DEFAULT 'fact'")
                db.execSQL("ALTER TABLE notes ADD COLUMN cardStatus TEXT NOT NULL DEFAULT 'open'")
                db.execSQL("ALTER TABLE notes ADD COLUMN who TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE notes ADD COLUMN statusChangedAt INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN snoozedUntil INTEGER")
                // Existing rows with a due date become due cards
                db.execSQL("UPDATE notes SET cardType = 'due' WHERE dueAt IS NOT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_cardStatus ON notes(cardStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_cardType ON notes(cardType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_dueAt ON notes(dueAt)")
            }
        }

        fun get(context: Context): MnemeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MnemeDatabase::class.java,
                    "mneme.db",
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
