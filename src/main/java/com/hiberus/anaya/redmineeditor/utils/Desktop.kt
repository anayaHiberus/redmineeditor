package com.hiberus.anaya.redmineeditor.utils

import java.awt.Desktop.*
import java.io.File
import java.net.URI

/**
 * Open this URI in the browser
 * @return true if it was opened, false on error
 */
fun URI.openInBrowser() =
    if (isDesktopSupported() && getDesktop().isSupported(Action.BROWSE)) {
        // browse using java (should work on Windows and gnome)
        daemonThread { getDesktop().browse(this) }
        true
    } else runCatching {
        // browse using the linux command (should work on KDE)
        Runtime.getRuntime().exec(arrayOf("xdg-open", this.toString())).waitFor() == 0
    }.onFailure { debugln(it) }.isSuccess

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
        daemonThread { getDesktop().open(this) }
    }.onFailure { debugln(it) }.isSuccess
