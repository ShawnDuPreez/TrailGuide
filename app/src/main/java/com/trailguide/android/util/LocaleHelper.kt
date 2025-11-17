package com.trailguide.android.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Helper class for managing app language/locale changes at runtime.
 */
object LocaleHelper {
    
    /**
     * Set the app locale to the specified language code.
     * @param context Application context
     * @param languageCode Language code (e.g., "en", "af", "zu")
     * @return Updated context with new locale
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        return updateResources(context, locale)
    }
    
    /**
     * Update context resources with new locale.
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Get current locale from context.
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * Get language code from context.
     */
    fun getCurrentLanguageCode(context: Context): String {
        return getCurrentLocale(context).language
    }
    
    /**
     * Apply locale to activity and recreate.
     */
    fun applyLocale(activity: Activity, languageCode: String) {
        setLocale(activity, languageCode)
        activity.recreate()
    }
}

