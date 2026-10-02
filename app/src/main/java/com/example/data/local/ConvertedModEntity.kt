package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "converted_mods")
data class ConvertedModEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modName: String,
    val originalFileName: String,
    val compatibilityScore: Int,
    val hasLuaScript: Boolean,
    val totalFiles: Int,
    val convertedFilesCount: Int,
    val summaryAr: String,
    val summaryEn: String,
    val timestamp: Long = System.currentTimeMillis()
)
