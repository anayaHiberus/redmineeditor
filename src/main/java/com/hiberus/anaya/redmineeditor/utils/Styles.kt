package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.model.AppSettings
import javafx.scene.Scene
import javafx.scene.control.Dialog
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.Window

/* ------------------------- functions ------------------------- */

/**
 * Sets the light/dark theme and icon
 */
fun Scene.stylize(asDark: Boolean = isDark) = stylesheets.run {
    // set theme
    if (asDark) put(DARK_STYLESHEET)
    else remove(DARK_STYLESHEET)

    // set icon
    (window as? Stage)?.icons?.put(icon)
}

/**
 * Sets the light/dark theme and icon
 */
fun Dialog<*>.stylize(asDark: Boolean = isDark) = dialogPane.scene.stylize(asDark)

/**
 * Stylize all displayed windows
 */
fun stylizeDisplayed() = runInForeground {
    Window.getWindows().map { it.scene }.distinct().letEach { stylize() }
}

/* ------------------------- property ------------------------- */

private val isDark get() = AppSettings.DARK_THEME.value.toBoolean() // the setting

private val DARK_STYLESHEET = Resources.getFile("dark.css").toExternalForm() // the stylesheet file

private val icon = Image(Resources.getImage("icon").openStream()) // the app icon
