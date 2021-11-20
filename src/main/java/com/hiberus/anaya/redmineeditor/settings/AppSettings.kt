package com.hiberus.anaya.redmineeditor.settings

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
        get() = DATA.get(name, default)
        set(value) {
            DATA.put(name, value)
        }

    /* ------------------------- settings functions ------------------------- */

    companion object {

        /**
         * loaded settings data
         */
        private val DATA = Preferences.userNodeForPackage(Main::class.java)
    }

}