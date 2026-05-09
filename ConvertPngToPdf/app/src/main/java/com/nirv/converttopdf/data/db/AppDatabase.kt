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
import com.nirv.converttopdf.data.db.entity.DocumentType

class DocumentTypeConverter {
    @TypeConverter fun fromType(type: DocumentType): String = type.name
    @TypeConverter fun toType(value: String): DocumentType =
        runCatching { DocumentType.valueOf(value) }.getOrDefault(DocumentType.PROJECT)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN status TEXT NOT NULL DEFAULT 'DRAFT'")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN type TEXT NOT NULL DEFAULT 'PROJECT'")
        db.execSQL("ALTER TABLE documents ADD COLUMN pdfPath TEXT")
        db.execSQL("ALTER TABLE documents ADD COLUMN parentProjectId INTEGER")
    }
}

@Database(
    entities     = [DocumentEntity::class, DocumentPageEntity::class],
    version      = 3,
    exportSchema = false
)
@TypeConverters(DocumentTypeConverter::class)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { INSTANCE = it }
            }
    }
}
