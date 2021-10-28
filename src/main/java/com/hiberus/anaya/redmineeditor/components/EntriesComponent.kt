package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.NoSelectionModel
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.util.Callback

/**
 * A list of entries that you can edit
 */
internal class EntriesComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var list: ListView<TimeEntry> // list view for displayed entries

    /* ------------------------- data ------------------------- */

    /**
     * items in listview
     */
    private val listItems = FXCollections.observableArrayList<TimeEntry>()

    @FXML
    private fun initialize() {
        // initialize list
        list.apply {
            // set items
            items = listItems
            // each entry is an entryComponent
            cellFactory = Callback { EntryComponent() }
            // disable selection
            selectionModel = NoSelectionModel()
        }

        // on new entries, display them
        AppController.onChanges(
            setOf(ChangeEvents.Entries, ChangeEvents.Day, ChangeEvents.Hours)
        ) { model: Model ->
            // clear and replace
            listItems.setAll(model.dayEntries ?: emptyList())
        }
    }
}