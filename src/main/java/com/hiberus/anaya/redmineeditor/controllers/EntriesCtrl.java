package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.cells.EntryCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;

import java.time.YearMonth;

public class EntriesCtrl implements InnerCtrl {

    public ChoiceBox<Integer> choice;

    @FXML
    ListView<TimeEntry> list;
    private final ObservableList<TimeEntry> listItems = FXCollections.observableArrayList();
    private final ObservableList<Integer> choiceItems = FXCollections.observableArrayList();
    private Model model;

    @FXML
    void initialize() {
        choice.setItems(choiceItems);
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCell());

        choice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Integer issue = choice.getValue();
            if (issue == null) return;
            model.hour_entries.get().createIssue(model.month.get().atDay(model.day.get()), issue);
            choice.setValue(null);
        });
    }

    @Override
    public void initCtrl(Model model) {
        this.model = model;
        model.month.observe(month ->
                replace(month, model.hour_entries.get(), model.day.get())
        );
        model.day.observe(day ->
                replace(model.month.get(), model.hour_entries.get(), day)
        );
        model.hour_entries.observeAndNotify(entries -> {
            replace(model.month.get(), entries, model.day.get());
            choiceItems.clear();
            choiceItems.addAll(entries.getAllIssues());
        });
    }

    private void replace(YearMonth month, Model.TimeEntries timeEntries, int day) {
        listItems.clear();
        if (day != 0)
            listItems.addAll(timeEntries.getEntriesForDate(month.atDay(day)));
    }

}
