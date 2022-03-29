package com.hiberus.anaya.redmineeditor.utils

import java.nio.file.Path

/**
 * Returns an existing file with the given path, relative to the executable directory; or null if not found
 */
fun getRelativeFile(path: String) =
    // in debug mode try first the personal folder
    if (System.getenv("DEBUG").toBoolean() and path.startsWith("conf/")) findFile(path.replaceFirst("conf/", "conf_personal/")) else null
    // try then the normal path
        ?: findFile(path)

/**
 * {@see getRelativeFile}
 */
private fun findFile(path: String) =
    Path.of(path).file // try directly
        ?: Path.of(System.getProperty("java.home"), path).file // try from the executable
        ?: Path.of(System.getProperty("java.class.path"), path).file // try from the java executable
        ?: Path.of(System.getProperty("user.dir"), path).file // try from the user directory

/**
 * Returns the file corresponding to this path only if it exists (logs it too)
 */
private val Path.file get() = this.toFile().also { println("Accessing file ${it.absolutePath} -> ${if (it.exists()) "OK" else "INVALID"}") }.takeIf { it.exists() }