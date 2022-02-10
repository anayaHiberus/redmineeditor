package com.hiberus.anaya.redmineeditor.utils

import java.awt.Desktop.*
import java.io.File
import java.net.URI
import kotlin.concurrent.thread

/**
 * Open this URI in the browser
 * @return true if it was opened, false on error
 */
fun URI.openInBrowser() =
    if (!isDesktopSupported() || !getDesktop().isSupported(Action.BROWSE)) {
        // not supported
        false
    } else runCatching {
        // browse
        thread(isDaemon = true) { getDesktop().browse(this) }
    }.onFailure { it.printStackTraceFix() }.isSuccess

/**
 * Opens this file in an external app/editor
 * @return true if it was opened, false on error
 */
fun File.openInApp() =
    if (!exists()) {
        // file doesn't exist
        false
    } else if (!isDesktopSupported() || !getDesktop().isSupported(Action.OPEN)) {
        // not supported
        false
    } else runCatching {
        // open
        thread(isDaemon = true) { getDesktop().open(this) }
    }.onFailure { it.printStackTraceFix() }.isSuccess
