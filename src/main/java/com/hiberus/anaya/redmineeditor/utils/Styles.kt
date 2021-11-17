package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.settings.AppSettings
import javafx.scene.Scene
import javafx.scene.control.Dialog
import javafx.scene.paint.Color

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
 * Changes the color to adapt to the current light/dark theme
 */
fun Color.stylize(asDark: Boolean = isDark): Color = if (asDark) darker() else brighter()

/* ------------------------- property ------------------------- */

private val isDark get() = AppSettings.DARK_THEME.value.toBoolean() // the setting

private val DARK_STYLESHEET = object {}.javaClass.getModuleResource("dark.css").toExternalForm() ?: "" // the stylesheet file
