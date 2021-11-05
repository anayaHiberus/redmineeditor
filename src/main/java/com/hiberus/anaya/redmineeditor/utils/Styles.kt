package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.controller.SETTING
import com.hiberus.anaya.redmineeditor.controller.value
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Dialog
import javafx.scene.paint.Color

/* ------------------------- functions ------------------------- */

/**
 * Sets the light/dark theme
 */
fun Scene.stylize() = stylesheets.run { if (isDark) add(DARK_STYLESHEET) else remove(DARK_STYLESHEET) }

/**
 * Sets the light/dark theme
 */
fun Parent.stylize() = stylesheets.run { if (isDark) add(DARK_STYLESHEET) else remove(DARK_STYLESHEET) }

/**
 * Sets the light/dark theme
 */
fun Dialog<*>.stylize() = dialogPane.stylize()

/**
 * Changes the color to adapt to the current light/dark theme
 */
fun Color.stylize(): Color = if (isDark) darker() else brighter()

/* ------------------------- property ------------------------- */

private val isDark get() = SETTING.DARK_THEME.value.toBoolean() // the setting

private val DARK_STYLESHEET = object {}.javaClass.getModuleResource("dark.css").toExternalForm() ?: "" // the stylesheet file
