package com.hiberus.anaya.redmineeditor.utils

import java.awt.Desktop.*
import java.net.URI

/**
 * Open this URI in the browser
 * @return true if it was opened, false on error
 */
fun URI.openInBrowser(): Boolean =
    if (!isDesktopSupported() || !getDesktop().isSupported(Action.BROWSE)) {
        // not supported
        false
    } else try {
        // supported, open
        getDesktop().browse(this)
        true
    } catch (e: Exception) {
        // exception
        e.printStackTrace()
        false
    }