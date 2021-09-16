package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import javafx.fxml.FXML;

/**
 * A list of action buttons
 */
public class ActionsCtrl implements InnerCtrl {

    // ------------------------- properties -------------------------

    private Model.TimeEntries hour_entries;

    // ------------------------- init -------------------------

    @Override
    public void initCtrl(Model model) {
        hour_entries = model.hour_entries;
    }

    // ------------------------- onActions -------------------------

    @FXML
    void onReload() {
        // press the reload button to reload the data
        hour_entries.reload();
    }
}
