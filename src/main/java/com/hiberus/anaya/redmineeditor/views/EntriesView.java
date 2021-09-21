package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.cells.EntryCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;

import java.util.Collection;
import java.util.List;

public class EntriesView extends InnerView {

    @FXML
    private ChoiceBox<Integer> choice; // the issues choicebox // TODO: move to another view
    @FXML
    private ListView<TimeEntry> list; // list view for displayed entries

    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList(); // items in listview
    private final ObservableList<Integer> choiceItems = FXCollections.observableArrayList(); // items in choicebox

    @FXML
    private void initialize() {
        // init
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCell(controller));
        choice.setItems(choiceItems);

        choice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // when selected issue, add entry
            Integer issue = choice.getValue();
            if (issue == null) return;
            controller.addEntryForCurrentDate(issue);
            choice.getSelectionModel().clearSelection(); // choice.setValue(null); // TODO: how to unselect??
        });
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

    /**
     * Display issues
     *
     * @param issues issues to display
     */
    public void setIssues(Collection<Integer> issues) {
        choiceItems.clear();
        choiceItems.addAll(issues);
    }

}
