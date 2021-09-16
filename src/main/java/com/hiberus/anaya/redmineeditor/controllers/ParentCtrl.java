package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.SimpleChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Controller for the parent app. Manages the loading indicator
 */
public class ParentCtrl {

    // ------------------------- model -------------------------

    private final Model model = new Model(); // this is the application model object

    // ------------------------- subcontrollers -------------------------

    @FXML
    SettingsCtrl settingsController;
    @FXML
    CalendarCtrl calendarController;
    @FXML
    SummaryCtrl summaryController;
    @FXML
    ActionsCtrl actionsController;

    // ------------------------- views -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

    // ------------------------- init -------------------------

    @FXML
    void initialize() {
        // init subcontrollers, like manual dependency injection
        calendarController.initCtrl(model);
        settingsController.initCtrl(model);
        summaryController.initCtrl(model);
        actionsController.initCtrl(model);

        SimpleChangeListener.register(model.hour_entries, (entries -> {
            // when entries are loading, show indicator and disable the rest
            boolean loading = model.hour_entries.isLoading();
            progress.setVisible(loading);
            parent.setDisable(loading);
        }));
    }


}
