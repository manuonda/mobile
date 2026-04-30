package com.nirv.converttopdf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nirv.converttopdf.data.db.dao.DocumentDao
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
import com.nirv.converttopdf.data.db.entity.DocumentStatus

class DocumentStatusConverter {
    @TypeConverter fun fromStatus(status: DocumentStatus): String = status.name
    @TypeConverter fun toStatus(value: String): DocumentStatus =
        runCatching { DocumentStatus.valueOf(value) }.getOrDefault(DocumentStatus.DRAFT)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN status TEXT NOT NULL DEFAULT 'DRAFT'")
    }
}

@Database(
    entities  = [DocumentEntity::class, DocumentPageEntity::class],
    version   = 2,
    exportSchema = false
)
@TypeConverters(DocumentStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "convert_pdf_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
    }
}
