package com.storm.lyricraft.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class LocaleManager(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)

    companion object {
        const val LANGUAGE_EN = "en"
        const val LANGUAGE_ZH = "zh"
        private const val KEY_LANGUAGE = "app_language"

        fun getCurrentLanguage(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Locale.getDefault().language
            } else {
                @Suppress("DEPRECATION")
                Locale.getDefault().language
            }
        }
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "") ?: ""
    }

    fun saveLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun applyLanguage(context: Context): Context {
        val lang = getLanguage()
        if (lang.isEmpty()) return context

        val locale = if (lang == LANGUAGE_ZH) Locale("zh") else Locale("en")
        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        return context.createConfigurationContext(config)
    }

    fun getDisplayLanguage(): String {
        return when (getLanguage()) {
            LANGUAGE_ZH -> "中文"
            LANGUAGE_EN -> "English"
            else -> {
                val sysLang = getCurrentLanguage()
                if (sysLang.startsWith("zh")) "中文" else "English"
            }
        }
    }
}