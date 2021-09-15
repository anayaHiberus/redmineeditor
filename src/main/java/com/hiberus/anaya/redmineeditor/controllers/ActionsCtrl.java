package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import javafx.fxml.FXML;

public class ActionsCtrl implements InnerCtrl {
    private ObservableProperty<Model.TimeEntries> hour_entries;

    @Override
    public void init(Model model) {
        hour_entries = model.hour_entries;
    }

    // ------------------------- reactions -------------------------

    @FXML
    void onReload() {
        hour_entries.get().loadEntries();
    }
}
