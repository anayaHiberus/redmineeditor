package com.hiberus.anaya.redmineeditor.settings

import com.hiberus.anaya.redmineeditor.utils.findFile
import java.util.*

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
        get() = DATA.getProperty(name, default)
        set(value) {
            DATA.setProperty(name, value)
        }

    /* ------------------------- settings functions ------------------------- */

    companion object {

        private val FILE = "conf/settings.properties"

        /**
         * loaded settings data
         */
        private val DATA = Properties()

        /**
         * Loads the settings from the properties file
         */
        fun load() =
            runCatching {
                DATA.clear()
                findFile(FILE).inputStream().use {
                    DATA.load(it)
                }
            }.onFailure {
                println(it)
                System.err.println("Settings file error!")
            }.isSuccess

        /**
         * Save the settings to the property file
         */
        fun save() =
            findFile(FILE).outputStream().use {
                DATA.store(it, null)
            }
    }

}