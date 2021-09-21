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

    public ChoiceBox<Integer> choice;

    @FXML
    ListView<TimeEntry> list;
    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList();
    private final ObservableList<Integer> choiceItems = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        choice.setItems(choiceItems);
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCell(controller));

        choice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // when selected entry, add issue
            Integer issue = choice.getValue();
            if (issue == null) return;
            controller.addIssueForCurrentDate(issue);
            choice.getSelectionModel().clearSelection(); // choice.setValue(null);
        });
    }

    @Override
    public void initView() {
    }

    public void clear() {
        listItems.clear();
    }

    public void setIssues(Collection<Integer> issues) {
        choiceItems.clear();
        choiceItems.addAll(issues);
    }

    public void replace(List<TimeEntry> rows) {
        clear();
        listItems.addAll(rows);
    }

}
