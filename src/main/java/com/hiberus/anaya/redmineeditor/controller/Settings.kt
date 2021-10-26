package com.hiberus.anaya.redmineeditor.controller

import com.hiberus.anaya.redmineeditor.utils.findFile
import java.util.*

/**
 * Global settings
 */
enum class SETTING(val default: String) {
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
    PREV_DAYS("7")
}

/**
 * this setting entry value
 */
val SETTING.value: String
    // extract from data
    get() = DATA.getProperty(name, default)

/**
 * true iff the settings were loaded
 */
var SettingsLoaded = false
    private set

/**
 * Loads the settings from the properties file
 */
fun LoadSettings() =
    runCatching {
        SettingsLoaded = false
        findFile("conf/settings.properties").inputStream().use {
            DATA.load(it)
            SettingsLoaded = true
        }
    }.onFailure {
        println(it)
        System.err.println("Settings file error!")
    }.isSuccess


/* ------------------------- private ------------------------- */

/**
 * loaded settings data
 */
private val DATA = Properties()
