package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.NoSelectionModel
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.util.Callback

/**
 * A list of entries that you can edit
 */
internal class EntriesComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var list: ListView<TimeEntry> // list view for displayed entries

    @FXML
    lateinit var filter: TextField // textbox to filter list

    @FXML
    lateinit var clearFilter: Button // button to clear filter

    /* ------------------------- data ------------------------- */

    /**
     * items in listview
     */
    private val itemsList = FXCollections.observableArrayList<TimeEntry>()
    private val filteredList = FilteredList(itemsList) { true }

    @FXML
    private fun initialize() {
        // initialize list
        list.apply {
            // set items
            items = filteredList
            // each entry is an entryComponent
            cellFactory = Callback { EntryComponent() }
            // disable selection
            selectionModel = NoSelectionModel()
        }

        // initialize filter
        filter.textProperty().addListener { _, _, value ->
            filteredList.setPredicate {
                if (value?.isNotEmpty() == true) it.issue.toString().contains(value, ignoreCase = true)
                else true
            }
        }
        // clearFilter is enabled when filter has text
        clearFilter.disableProperty().bind(filter.textProperty().isEmpty)

        // on new entries, display them
        AppController.onChanges(
            setOf(ChangeEvents.EntryList, ChangeEvents.Day)
        ) { model: Model ->
            // clear and replace
            itemsList.setAll(model.dayEntries ?: emptyList())
            clearFilter()
        }

        // when entry change, update
        AppController.onChanges(setOf(ChangeEvents.EntryContent)) {
            list.lookupAll(".cell").forEach {
                if (it is EntryComponent) it.update()
            }
        }

        // when issue change, update
        AppController.onChanges(setOf(ChangeEvents.IssueContent)) {
            list.lookupAll(".cell").forEach {
                if (it is EntryComponent) {
                    it.update()
                }
            }
        }

    }

    @FXML
    private fun clearFilter() = filter.clear()
}