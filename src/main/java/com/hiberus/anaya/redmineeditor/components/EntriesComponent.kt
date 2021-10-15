package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
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
internal class EntriesComponent : BaseComponent() {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var list: ListView<TimeEntry> // list view for displayed entries

    private val listItems = FXCollections.observableArrayList<TimeEntry>() // items in listview

    @FXML
    private fun initialize() {
        // javafx init
        list.apply {
            items = listItems // set items
            cellFactory = Callback { EntryComponent(controller) } // each entry is an entryComponent
            selectionModel = NoSelectionModel() // disable selection
        }
    }

    public override fun init() {
        // on new entries, display them
        controller.register(
            setOf(ChangeEvents.Entries, ChangeEvents.Day, ChangeEvents.Hours)
        ) { model: Model ->
            // clear and replace
            listItems.setAll(model.dayEntries)
        }
    }
}