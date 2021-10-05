package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.NoSelectionModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.Set;

/**
 * A list of entries that you can edit
 */
public class EntriesCtrl extends InnerCtrl {

    @FXML
    private ListView<TimeEntry> list; // list view for displayed entries

    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList(); // items in listview

    @FXML
    private void initialize() {
        // init
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCtrl(model));

        list.setSelectionModel(new NoSelectionModel<>());
    }

    @Override
    void init() {
        // on new entries, display them
        model.notificator.register(Set.of(Model.Events.Entries, Model.Events.Day, Model.Events.Hours), () -> {
            // clear and replace
            listItems.setAll(model.getDayEntries());
        });
    }

}
