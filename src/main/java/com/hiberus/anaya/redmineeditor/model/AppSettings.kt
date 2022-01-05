package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineeditor.Main
import java.util.prefs.Preferences

/**
 * Global settings
 */
enum class AppSettings(val default: String) {
    /**
     * Redmine url
     */
    URL(""),

    /**
     * Redmine key
     */
    KEY(""),

    /**
     * Disable PUT/POST
     */
    READ_ONLY("false"),

    /**
     * Automatically load total hours
     */
    AUTO_LOAD_TOTAL_HOURS("true"),

    /**
     * Number of days for 'past' computations
     */
    PREV_DAYS("7"),

    /**
     * Dark theme
     */
    DARK_THEME("false"),
    ;

    /* ------------------------- properties functions ------------------------- */

    /**
     * this setting entry value
     */
    var value: String
        get() = PREFS.get(name, default)
        set(value) {
            if (value != default) PREFS.put(name, value)
            else PREFS.remove(name) // don't save default
        }

    /**
     * Same as [setValue], but returns true if the new value is different
     */
    fun modify(newValue: Any) = newValue.toString().let { (it != value).apply { value = it } }

}


/**
 * loaded settings preferences
 */
private val PREFS = Preferences.userNodeForPackage(Main::class.java)
