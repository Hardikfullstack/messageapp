package com.messages.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import com.messages.utils.AnalyticsManager

data class LanguageOption(val code: String, val englishName: String, val nativeName: String)

val AppLanguages = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("hi", "Hindi", "हिन्दी"),
    LanguageOption("ar", "Arabic", "العربية"),
    LanguageOption("fr", "French", "Français"),
    LanguageOption("de", "German", "Deutsch"),
    LanguageOption("id", "Indonesian", "Bahasa Indonesia"),
    LanguageOption("it", "Italian", "Italiano"),
    LanguageOption("pt", "Portuguese", "Português"),
    LanguageOption("es", "Spanish", "Español")
)

object LanguageState {
    private const val PREFS = "messages_prefs"
    private const val KEY_LANGUAGE_CODE = "app_language_code"

    var code = mutableStateOf("en")

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        val saved = readCode(context)
        code.value = saved
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
        AnalyticsManager.setUserProperty("app_language", saved)
    }

    fun setLanguage(context: Context, newCode: String) {
        code.value = newCode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_CODE, newCode)
            .apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newCode))
        AnalyticsManager.setUserProperty("app_language", newCode)
    }

    fun englishNameFor(code: String): String =
        AppLanguages.firstOrNull { it.code == code }?.englishName ?: "English"

    private fun readCode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_CODE, "en") ?: "en"
    }
}
