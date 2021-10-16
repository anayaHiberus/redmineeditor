package com.hiberus.anaya.redmineeditor.controller

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Global settings
 */
enum class ENTRY {
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
 * Returns the setting entry
 *
 * @param entry entry to return
 * @return that entry value
 */
fun get(entry: ENTRY): String = data[entry.name]
// TODO: better defaults
    ?: "".also { System.err.println("No configuration with entry ${entry.name} is present.") }

/* ------------------------- load on init ------------------------- */

// Load settings from hardcoded file
private const val filename = "/home/anaya/abel/personal/proyectos/redmine/settings.conf"

private val data = runCatching {
    Files.lines(Paths.get(filename)).use { lines ->
        // remove comments
        lines.map { line -> line.replace("#.*".toRegex(), "") }
            // skip empty
            .filter { it.isNotBlank() }
            // split by sign
            .map { it.split("=") }
            // remove invalid
            .filter {
                when (it.size) {
                    2 -> true
                    else -> false.apply { System.err.println("Invalid entry in settings: ${it.joinToString("=")}") }
                }
            }
            // save to map
            .map { it[0] to it[1] }.toList().toMap()
    }
}.onFailure {
    print(it)
    System.err.println("Settings file error!")
}.getOrElse { emptyMap() }
