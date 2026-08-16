package com.storm.lyricraft.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE

class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private const val KEY_THEME = "theme_mode"
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }
}