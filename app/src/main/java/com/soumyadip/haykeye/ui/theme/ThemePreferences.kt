package com.soumyadip.haykeye.ui.theme

import android.content.Context
import android.content.SharedPreferences

enum class AppTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System");

    companion object {
        fun fromName(name: String?): AppTheme {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: DARK
        }
    }
}

class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getTheme(): AppTheme {
        val themeName = prefs.getString(KEY_THEME_MODE, AppTheme.DARK.name)
        return AppTheme.fromName(themeName)
    }

    fun saveTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME_MODE, theme.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "haykeye_theme_prefs"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}
