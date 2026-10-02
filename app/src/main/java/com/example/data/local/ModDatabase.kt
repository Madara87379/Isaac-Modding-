package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ConvertedModEntity::class], version = 1, exportSchema = false)
abstract class ModDatabase : RoomDatabase() {
    abstract fun convertedModDao(): ConvertedModDao

    companion object {
        @Volatile
        private var INSTANCE: ModDatabase? = null

        fun getInstance(context: Context): ModDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ModDatabase::class.java,
                    "isaac_mods_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
