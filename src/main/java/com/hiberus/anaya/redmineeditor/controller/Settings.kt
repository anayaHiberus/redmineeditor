package com.hiberus.anaya.redmineeditor.controller

import com.hiberus.anaya.redmineeditor.utils.findFile
import java.util.*

/**
 * Global settings
 */
enum class SETTING {
    /**
     * Redmine url
     */
    URL,

    /**
     * Redmine key
     */
    KEY
}

/**
 * this setting entry value
 */
val SETTING.value
    // extract from data
    get() = DATA.getProperty(name, "") // TODO: better defaults

/**
 * true iff the settings were loaded
 */
var settingsLoaded = false
    private set

/* ------------------------- private ------------------------- */

/**
 * loaded settings data
 */
private val DATA = Properties().apply {
    runCatching {
        findFile("conf/settings.properties").inputStream().use {
            load(it)
            settingsLoaded = true
        }
    }.onFailure {
        println(it)
        System.err.println("Settings file error!")
    }
}
