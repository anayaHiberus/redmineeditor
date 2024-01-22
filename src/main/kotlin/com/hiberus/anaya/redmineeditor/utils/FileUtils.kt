package com.hiberus.anaya.redmineeditor.utils

import java.io.File
import java.nio.file.Path

/**
 * Returns an existing file with the given path, relative to the executable directory; or null if not found
 */
fun getRelativeFile(path: String) =
    // try first the personal folder
    findFile(path.personalFolder)
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
 * Returns all files from the given [path], optionally filtered by a custom [filter]
 */
fun getAllFiles(path: String, filter: (File, String) -> Boolean = { _, _ -> true }) =
    listOf(path.personalFolder, path)
        .mapNotNull { findFile(it) }
        .mapNotNull { it.list(filter) }
        .ifEmpty {
            debugln("Path [$path] not found or doesn't contain any files for filter")
            listOf(emptyArray<String>())
        }.flatMap { it.toList() }

/**
 * Returns the file corresponding to this path only if it exists (logs it too)
 */
private val Path.file get() = toFile().also { debugln("Accessing file ${it.absolutePath} -> ${if (it.exists()) "OK" else "INVALID"}") }.takeIf { it.exists() }

/** Replaces the config folder with the personal one. */
private val String.personalFolder get() = replaceFirst("conf/", "conf_personal/")