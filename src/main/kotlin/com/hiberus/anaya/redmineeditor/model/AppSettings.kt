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
     * Automatically load assigned issues
     */
    AUTO_LOAD_ASSIGNED("true"),

    /**
     * Number of days for 'past' computations
     */
    PREV_DAYS("7"),

    /**
     * Assigned issues with update_time older than [this] days are ignored.
     */
    IGNORE_OLD_ASSIGNED("365"),

    /**
     * Dark theme
     */
    DARK_THEME("false"),

    /**
     * Check for updates on start
     */
    CHECK_UPDATES("true"),

    /**
     * Check for schedule updates on start
     */
    CHECK_SCHEDULE_UPDATES("true"),

    /**
     * Schedule file of user (to obtain holidays)
     */
    SCHEDULE_FILE("Zaragoza"),

    /**
     * How to mark used entries
     */
    MARK_USED(MarkUsed.OPACITY.name)
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
    fun modify(newValue: String) = newValue.let { (it != value).apply { value = it } }

}


/* ------------------------- specific ------------------------- */

/**
 * For MARK_USED setting
 */
enum class MarkUsed {
    OPACITY,
    COLOR,
    NONE,
    ;
}

/**
 * Return the Mark_USED Setting as enum
 */
val MarkUsedSetting get() = MarkUsed.valueOf(AppSettings.MARK_USED.value.uppercase())


/* ------------------------- internal ------------------------- */

/**
 * loaded settings preferences
 */
private val PREFS = Preferences.userNodeForPackage(Main::class.java)
