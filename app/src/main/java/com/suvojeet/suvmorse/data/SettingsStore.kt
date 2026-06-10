package com.suvojeet.suvmorse.data

import android.content.Context

/** Lightweight persistence for user preferences, backed by SharedPreferences. */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("suvmorse_settings", Context.MODE_PRIVATE)

    var wpm: Int
        get() = prefs.getInt(KEY_WPM, 15)
        set(value) = prefs.edit().putInt(KEY_WPM, value).apply()

    var frequency: Float
        get() = prefs.getFloat(KEY_FREQ, 700f)
        set(value) = prefs.edit().putFloat(KEY_FREQ, value).apply()

    var torch: Boolean
        get() = prefs.getBoolean(KEY_TORCH, false)
        set(value) = prefs.edit().putBoolean(KEY_TORCH, value).apply()

    var haptic: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, false)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var loop: Boolean
        get() = prefs.getBoolean(KEY_LOOP, false)
        set(value) = prefs.edit().putBoolean(KEY_LOOP, value).apply()

    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()

    var silent: Boolean
        get() = prefs.getBoolean(KEY_SILENT, false)
        set(value) = prefs.edit().putBoolean(KEY_SILENT, value).apply()

    /** 0 = follow system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME, 0)
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()

    private companion object {
        const val KEY_WPM = "wpm"
        const val KEY_FREQ = "frequency"
        const val KEY_TORCH = "torch"
        const val KEY_HAPTIC = "haptic"
        const val KEY_LOOP = "loop"
        const val KEY_SENSITIVITY = "sensitivity"
        const val KEY_SILENT = "silent"
        const val KEY_THEME = "theme_mode"
    }
}
