package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.model.ProjectColor
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell
import javafx.fxml.FXML
import javafx.scene.control.ColorPicker
import javafx.scene.control.TextField

/** One of the entries in the entries list */
class ProjectColorCell(val onChangeListener: () -> Unit) : SimpleListCell<ProjectColor>(ResourceLayout("project_color_cell")) {

    /* ------------------------- views ------------------------- */

    lateinit var colorPicker: ColorPicker
    lateinit var regexTxt: TextField

    /* ------------------------- init ------------------------- */

    override fun update() {
        item?.let { projectColor ->
            colorPicker.value = projectColor.color
            regexTxt.text = projectColor.regex.pattern
        }
    }

    /* ------------------------- actions ------------------------- */

    @FXML
    fun remove() = listView.items.remove(item)

    @FXML
    fun onChange() = onChangeListener()
}