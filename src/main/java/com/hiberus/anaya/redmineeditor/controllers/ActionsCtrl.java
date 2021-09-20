package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import javafx.fxml.FXML;

import java.time.YearMonth;

/**
 * A list of action buttons
 */
public class ActionsCtrl implements InnerCtrl {

    // ------------------------- properties -------------------------

    private ObservableProperty<Model.TimeEntries> hour_entries;
    private ObservableProperty<YearMonth> month;

    // ------------------------- init -------------------------

    @Override
    public void initCtrl(Model model) {
        hour_entries = model.hour_entries;
        month = model.month;
    }

    // ------------------------- onActions -------------------------

    @FXML
    void reload() {
        // press the reload button to reload the data
        hour_entries.get().clear();
        hour_entries.get().loadMonth(month.get());
    }

    @FXML
    void update() {
        // update changes
        hour_entries.get().update();
        // and reload
        reload();
    }
}
