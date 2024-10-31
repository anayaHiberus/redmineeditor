package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.PROJECT_COLORS_FROM_FILE
import com.hiberus.anaya.redmineeditor.model.ProjectColor
import com.hiberus.anaya.redmineeditor.utils.NoSelectionModel
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.scene.control.ListView
import javafx.scene.paint.Color
import javafx.util.Callback

/** Controller for the project colors entries. */
class ProjectColorConfiguration(private val listView: ListView<ProjectColor>) {
    private var onChangeListener: (() -> Unit)? = null

    private val projectColors = FXCollections.observableArrayList<ProjectColor>()

    init {
        // sync height
        projectColors.addListener(ListChangeListener {
            // set height based on the amount of items. Must be synchronized
            listView.prefHeight = it.list.size * /*cell height=*/35.0 + /*padding=*/2
        })

        listView.apply {
            // set items
            items = projectColors
            // cell creator
            cellFactory = Callback { ProjectColorCell { onChangeListener?.let { it() } } }
            // disable selection
            selectionModel = NoSelectionModel()
        }

        // initialize values
        projectColors.addAll(PROJECT_COLORS_FROM_FILE)
    }

    fun addProjectColor() = projectColors.add(ProjectColor(Regex(".*"), Color.TRANSPARENT))

    fun onChange(listener: () -> Unit) {
        onChangeListener = listener
    }

}
