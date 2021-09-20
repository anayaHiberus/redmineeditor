package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.cells.EntryCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;

import java.time.LocalDate;

public class EntriesCtrl extends InnerCtrl {

    public ChoiceBox<Integer> choice;

    @FXML
    ListView<TimeEntry> list;
    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList();
    private final ObservableList<Integer> choiceItems = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        choice.setItems(choiceItems);
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCell(() -> model.notifyChanged()));

        choice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // when selected entry, add issue
            Integer issue = choice.getValue();
            if (issue == null) return;
            LocalDate date = model.getDate();
            if (date != null) model.hour_entries.createIssue(date, issue);
            choice.setValue(null);
        });
    }

    @Override
    public void initCtrl() {
        model.onChanges(() -> {
            // replace entries
            replace();
            // populate issues
            choiceItems.clear();
            choiceItems.addAll(model.hour_entries.getAllIssues());
        });
    }

    private void replace() {
        listItems.clear();
        LocalDate date = model.getDate();
        if (date != null) {
            model.hour_entries.prepareEntriesForDate(date);
            listItems.addAll(model.hour_entries.getEntriesForDate(date));
        }
    }

}
