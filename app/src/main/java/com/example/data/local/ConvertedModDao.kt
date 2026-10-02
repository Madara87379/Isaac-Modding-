package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConvertedModDao {
    @Query("SELECT * FROM converted_mods ORDER BY timestamp DESC")
    fun getAllConvertedMods(): Flow<List<ConvertedModEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConvertedMod(mod: ConvertedModEntity): Long

    @Query("DELETE FROM converted_mods WHERE id = :id")
    suspend fun deleteMod(id: Long)

    @Query("DELETE FROM converted_mods")
    suspend fun clearAll()
}
