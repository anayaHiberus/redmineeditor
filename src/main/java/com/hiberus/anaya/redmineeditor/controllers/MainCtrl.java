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
        // init subcontrollers
        calendarController.init(model);
        settingsController.init(model);
        summaryController.init(model);
        actionsController.init(model);

        // loading
        model.hour_entries.registerObserver(entries -> {
            boolean loading = entries.isLoading();
            progress.setVisible(loading);
            parent.setDisable(loading);
        });
    }

    // ------------------------- loading -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

}
