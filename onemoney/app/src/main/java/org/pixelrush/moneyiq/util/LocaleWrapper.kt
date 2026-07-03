package org.syalosovetskyi.onemoney.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app locale support. The chosen language tag is kept in a small synchronous
 * SharedPreferences store so it can be read inside Activity.attachBaseContext()
 * (which runs before Hilt injection). "default" means follow the system locale.
 *
 * Flow: on language change call [setLang] (synchronous commit) then Activity.recreate();
 * recreate() re-runs attachBaseContext(), which calls [wrap] to apply the locale.
 */
object LocaleWrapper {

    private const val PREFS = "locale_prefs"
    private const val KEY   = "app_lang"
    const val DEFAULT       = "default"

    fun getLang(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, DEFAULT) ?: DEFAULT

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, lang).commit()   // commit(): must be visible before recreate()
    }

    /** Wraps [base] with the stored locale. Returns [base] unchanged for "default". */
    fun wrap(base: Context): Context {
        val lang = getLang(base)
        if (lang == DEFAULT) return base
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
