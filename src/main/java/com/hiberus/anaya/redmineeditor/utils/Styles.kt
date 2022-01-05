package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.model.AppSettings
import javafx.scene.Scene
import javafx.scene.control.Dialog
import javafx.stage.Window

/* ------------------------- functions ------------------------- */

/**
 * Sets the light/dark theme
 */
fun Scene.stylize(asDark: Boolean = isDark) = stylesheets.run {
    when {
        asDark && contains(DARK_STYLESHEET) -> false
        asDark -> add(DARK_STYLESHEET)
        else -> remove(DARK_STYLESHEET)
    }
}

/**
 * Sets the light/dark theme
 */
fun Dialog<*>.stylize(asDark: Boolean = isDark) = dialogPane.scene.stylize(asDark)

/**
 * Stylize all displayed windows
 */
fun stylizeDisplayed() = runInForeground {
    Window.getWindows().map { it.scene }.distinct().forEach { it.stylize() }
}

/* ------------------------- property ------------------------- */

private val isDark get() = AppSettings.DARK_THEME.value.toBoolean() // the setting

private val DARK_STYLESHEET = Resources.getFile("dark.css").toExternalForm() // the stylesheet file
