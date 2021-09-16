package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.cells.EntryCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.time.YearMonth;

public class EntriesCtrl implements InnerCtrl {

    @FXML
    ListView<Model.TimeEntries.TimeEntry> list;
    private final ObservableList<Model.TimeEntries.TimeEntry> listItems = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        list.setItems(listItems);
        list.setCellFactory(param -> new EntryCell());
    }

    @Override
    public void initCtrl(Model model) {
        model.month.observe(month ->
                replace(month, model.hour_entries.get(), model.day.get())
        );
        model.day.observe(day ->
                replace(model.month.get(), model.hour_entries.get(), day)
        );
        model.hour_entries.observeAndNotify(entries ->
                replace(model.month.get(), entries, model.day.get())
        );
    }

    private void replace(YearMonth month, Model.TimeEntries timeEntries, int day) {
        listItems.clear();
        if (day != 0)
            listItems.addAll(timeEntries.getEntriesForDate(month.atDay(day)));
    }

}
