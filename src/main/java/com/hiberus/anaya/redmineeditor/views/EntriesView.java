package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.cells.EntryCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.List;

/**
 * A list of entries that you can edit
 */
public class EntriesView extends InnerView {

    @FXML
    private ListView<TimeEntry> list; // list view for displayed entries

    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList(); // items in listview

    @FXML
    private void initialize() {
        // init
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCell(controller));
    }

    /**
     * Remove displayed entries
     */
    public void clear() {
        listItems.clear();
    }

    /**
     * Sets displayed entries
     *
     * @param entries entries to display
     */
    public void replace(List<TimeEntry> entries) {
        clear();
        listItems.addAll(entries);
    }

}
