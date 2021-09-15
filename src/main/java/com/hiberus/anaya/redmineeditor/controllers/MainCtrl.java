package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class MainCtrl {

    // ------------------------- model -------------------------

    private final Model model = new Model();

    // ------------------------- subcontrollers -------------------------

    @FXML
    SettingsCtrl settingsController;
    @FXML
    CalendarCtrl calendarController;
    @FXML
    SummaryCtrl summaryController;
    @FXML
    ActionsCtrl actionsController;

    // ------------------------- reactions -------------------------

    @FXML
    void initialize() {
        // init
        calendarController.init(model);
        settingsController.init(model);
        summaryController.init(model);
        actionsController.init(model);

        model.hour_entries.registerObserver(newValue -> {
            boolean loading = newValue.isLoading();
            progress.setVisible(loading);
            parent.setDisable(loading);
        });

        model.month.registerObserver(newValue -> {
            model.hour_entries.get().loadEntries();
        });
    }

    // ------------------------- loading -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

}
