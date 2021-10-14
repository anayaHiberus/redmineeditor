package com.hiberus.anaya.redmineeditor.utils

import java.awt.Desktop
import java.net.URI

/**
 * Desktop utilities
 */
object Desktop {

    /**
     * Open a page in the browser
     *
     * @param url page to open
     * @return true if it was opened, false on error
     */
    @JvmStatic
    fun openInBrowser(url: String): Boolean =
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            // not supported
            false
        } else try {
            // supported, open
            Desktop.getDesktop().browse(URI(url))
            true
        } catch (e: Exception) {
            // exception
            e.printStackTrace()
            false
        }
}