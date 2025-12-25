package com.example.soukify.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

/**
 * Helper class for managing app locale and language changes
 * Handles setting and applying language preferences to the app
 */
public class LocaleHelper {
    
    private static final String PREFS_NAME = "com.example.soukify.locale_prefs";
    private static final String LANGUAGE_KEY = "selected_language";
    private static final String DEFAULT_LANGUAGE = "en";
    
    /**
     * Get the saved language preference
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LANGUAGE_KEY, DEFAULT_LANGUAGE);
    }
    
    /**
     * Save language preference locally
     */
    public static void setLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LANGUAGE_KEY, language).apply();
    }
    
    /**
     * Apply language to context (for creating localized context)
     * This is the modern way to handle locales in Android
     */
    public static Context onAttach(Context context) {
        String lang = getLanguage(context);
        if ("device".equalsIgnoreCase(lang)) {
            lang = getDeviceLanguage();
        }
        return applyLanguage(context, lang);
    }

    /**
     * Apply language to context
     */
    public static Context applyLanguage(Context context, String language) {
        setLanguage(context, language);
        Locale locale = getLocaleFromCode(language);
        return updateResources(context, locale);
    }
    
    /**
     * Update the app's locale configuration (Legacy support)
     */
    public static void updateLocale(Context context, String language) {
        Locale locale = getLocaleFromCode(language);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
        } else {
            config.locale = locale;
        }
        
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
    
    /**
     * Convert language code to Locale
     */
    private static Locale getLocaleFromCode(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "fr":
                return new Locale("fr");
            case "ar":
                return new Locale("ar");
            case "en":
            default:
                return new Locale("en");
        }
    }
    
    /**
     * Helper method to update resources with new locale
     */
    private static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }
    
    /**
     * Check if device language is being used
     */
    public static boolean isDeviceLanguage(Context context) {
        String language = getLanguage(context);
        return "device".equalsIgnoreCase(language);
    }
    
    /**
     * Get device default language
     */
    public static String getDeviceLanguage() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        
        // Map to supported languages
        if ("fr".equals(language)) {
            return "fr";
        } else if ("ar".equals(language)) {
            return "ar";
        }
        
        return "en";
    }
}
