package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("isaac_porter_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_user_api_key"
        private const val KEY_MODEL = "gemini_selected_model"
        private const val KEY_IS_ARABIC = "pref_is_arabic"
    }

    fun getApiKey(): String {
        val saved = prefs.getString(KEY_API_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved
        // Fallback to BuildConfig if available
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getModel(): String {
        return prefs.getString(KEY_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
    }

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun isArabic(): Boolean {
        return prefs.getBoolean(KEY_IS_ARABIC, true)
    }

    fun setArabic(isArabic: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ARABIC, isArabic).apply()
    }
}
