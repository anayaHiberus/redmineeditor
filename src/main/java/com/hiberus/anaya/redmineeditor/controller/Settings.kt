package com.hiberus.anaya.redmineeditor.controller

import java.io.File

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
    get() = DATA.getOrElse(name) {
        // or return default otherwise
        System.err.println("No configuration with entry $name is present.")
        "" // TODO: better defaults
    }

/* ------------------------- private ------------------------- */

/**
 * hardcoded file to load settings from
 */
private const val filename = "/home/anaya/abel/personal/proyectos/redmine/settings.conf"

/**
 * loaded settings data
 */
private val DATA = runCatching {
    File(filename).readLines().asSequence()
        // remove comments
        .map { it.replace("#.*".toRegex(), "") }
        // skip empty
        .filter { it.isNotBlank() }
        // split by sign
        .map { line -> line.split("=", limit = 2).map { it.trim() } to line }
        // build valid to map
        .mapNotNull { (data, line) ->
            if (data.size == 2) data[0] to data[1] // ok
            else {
                // invalid
                System.err.println("Invalid entry in settings, missing equal sign: $line")
                null
            }
        }.toMap()
}.onFailure {
    print(it)
    System.err.println("Settings file error!")
}.getOrElse { emptyMap() }
