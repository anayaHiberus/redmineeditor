package com.hiberus.anaya.redmineeditor.utils

import java.io.File
import java.nio.file.Path

/**
 * Tries to find an existing file relative to the executable directory
 */
fun findFile(path: String) =
    Path.of(path).file // try directly
        ?: Path.of(System.getProperty("java.home"), path).file // try from the executable
        ?: Path.of(System.getProperty("java.class.path"), path).file // try from the java executable
        ?: Path.of(System.getProperty("user.dir"), path).file // try from the user directory
        ?: File(path) // just fail

/**
 * Returns the file corresponding to this path only if it exists (logs it too)
 */
private val Path.file get() = this.toFile().also { println("Accessing file ${it.absolutePath} -> ${if (it.exists()) "OK" else "INVALID"}") }.takeIf { it.exists() }