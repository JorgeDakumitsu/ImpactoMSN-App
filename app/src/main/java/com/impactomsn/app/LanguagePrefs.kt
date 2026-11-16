package com.impactomsn.app

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** One selectable app language. [tag] is a BCP-47 tag, or empty for "follow the system". */
data class AppLanguage(val tag: String, val nativeName: String)

/**
 * In-app language selection. Android 13+ uses the per-app language API (so the choice also shows in
 * system settings); older versions apply the stored tag by wrapping each activity's base context.
 */
object LanguagePrefs {
    private const val PREFS = "impactomsn_language"
    private const val KEY_TAG = "tag"
    const val AUTOMATIC = ""

    /** Native names are never translated, so people can always find their own language. */
    val languages = listOf(
        AppLanguage("es", "Español (España)"),
        AppLanguage("es-419", "Español (Latinoamérica)"),
        AppLanguage("pt", "Português (Portugal)"),
        AppLanguage("pt-BR", "Português (Brasil)"),
        AppLanguage("it", "Italiano"),
        AppLanguage("fr", "Français"),
        AppLanguage("en-US", "English (US)"),
        AppLanguage("en-GB", "English (UK)"),
        AppLanguage("de", "Deutsch"),
    )

    fun current(context: Context): String {
        val stored = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, AUTOMATIC).orEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val fromSystem = runCatching {
                context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
            }.getOrDefault(stored)
            return languages.firstOrNull { it.tag.equals(fromSystem, ignoreCase = true) }?.tag ?: AUTOMATIC
        }
        return languages.firstOrNull { it.tag == stored }?.tag ?: AUTOMATIC
    }

    /** Persists and applies [tag]. Returns true when the caller must recreate its activity itself. */
    fun set(context: Context, tag: String): Boolean {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_TAG, tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            return false // the system recreates the activities
        }
        return true
    }

    /** Wrap in attachBaseContext. A no-op on Android 13+ and while following the system language. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, AUTOMATIC).orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = android.content.res.Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return LanguageContext(base.createConfigurationContext(configuration))
    }

    private class LanguageContext(base: Context) : ContextWrapper(base)
}
