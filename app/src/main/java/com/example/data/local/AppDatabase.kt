package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        TaskExceptionEntity::class,
        OccurrenceStateEntity::class,
        SubtaskEntity::class,
        SubtaskStateEntity::class,
        BacklogEntity::class,
        CategoryEntity::class,
        ScheduledAlarmEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskExceptionDao(): TaskExceptionDao
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun backlogDao(): BacklogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "power_of_now.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).categoryDao().insertCategories(DEFAULT_CATEGORIES)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("study", "دراسة", "📚", 0xFF3B82F6, isDefault = true),
            CategoryEntity("work", "عمل", "💼", 0xFF6366F1, isDefault = true),
            CategoryEntity("quran", "قرآن", "📖", 0xFF10B981, isDefault = true),
            CategoryEntity("fitness", "رياضة", "🏋️", 0xFFF97316, isDefault = true),
            CategoryEntity("rest", "راحة", "😌", 0xFF06B6D4, isDefault = true),
            CategoryEntity("food", "طعام", "🍽️", 0xFFEAB308, isDefault = true),
            CategoryEntity("prayer", "صلاة", "🕌", 0xFF14B8A6, isDefault = true),
            CategoryEntity("coding", "برمجة", "💻", 0xFF8B5CF6, isDefault = true),
            CategoryEntity("reading", "قراءة", "📕", 0xFFEC4899, isDefault = true),
            CategoryEntity("sleep", "نوم", "😴", 0xFF64748B, isDefault = true),
            CategoryEntity("other", "أخرى", "⭐", 0xFFF59E0B, isDefault = true)
        )
    }
}
