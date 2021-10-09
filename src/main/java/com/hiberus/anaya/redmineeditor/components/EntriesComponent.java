package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.utils.NoSelectionModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.Set;

import static com.hiberus.anaya.redmineeditor.model.ChangeEvents.*;

/**
 * A list of entries that you can edit
 */
public class EntriesComponent extends BaseComponent {

    @FXML
    private ListView<TimeEntry> list; // list view for displayed entries

    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList(); // items in listview

    @FXML
    private void initialize() {
        // init
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryComponent(controller));

        list.setSelectionModel(new NoSelectionModel<>());
    }

    @Override
    void init() {
        // on new entries, display them
        controller.register(Set.of(Entries, Day, Hours), model -> {
            // clear and replace
            listItems.setAll(model.getDayEntries());
        });
    }

}
