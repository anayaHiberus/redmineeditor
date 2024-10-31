package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.Colors
import com.hiberus.anaya.redmineeditor.utils.enabled
import javafx.scene.control.Button
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.control.Tooltip

/** Controller for the app colors entries. */
class AppColorEntryController {

    lateinit var defaultBtn: Button
    lateinit var restoreBtn: Button
    lateinit var colorPicker: ColorPicker
    lateinit var label: Label

    lateinit var color: Colors

    fun initialize(color: Colors) {
        this.color = color

        // init properties
        label.text = color.description
        label.tooltip = Tooltip(color.name)
        colorPicker.value = color.value

        // init buttons
        colorPicker.valueProperty().addListener { _, _, value ->
            restoreBtn.enabled = value != color.value
            defaultBtn.enabled = value != color.defaultValue
        }
        restoreBtn.enabled = false
        defaultBtn.enabled = color.value != color.defaultValue
    }

    /** True iff the user changed the color. */
    val hasChanges get() = colorPicker.value != color.value

    /** Restores the currently saved color. */
    fun restoreCurrent() {
        colorPicker.value = color.value
    }

    /** Applies the chosen color, returns true iff it was changed. */
    fun modify() = (colorPicker.value != color.value).also { color.value = colorPicker.value }

    /** Loads the default color. */
    fun loadDefault() {
        colorPicker.value = color.defaultValue
    }

    /** Run listener when color changes. */
    fun onChange(listener: () -> Unit) {
        colorPicker.valueProperty().addListener { _, _, _ -> listener() }
    }
}
