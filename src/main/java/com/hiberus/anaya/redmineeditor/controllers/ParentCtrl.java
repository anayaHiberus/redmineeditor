package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
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
    CalendarCtrl calendarController;
    @FXML
    SummaryCtrl summaryController;
    @FXML
    ActionsCtrl actionsController;
    @FXML
    EntriesCtrl entriesController;

    // ------------------------- views -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

    // ------------------------- init -------------------------

    @FXML
    void initialize() {
        // init subcontrollers, like manual dependency injection
        calendarController.injectModel(model);
        summaryController.injectModel(model);
        actionsController.injectModel(model);
        entriesController.injectModel(model);

        model.onChanges(() -> {
            // when entries are loading, show indicator and disable the rest
            boolean loading = model.hour_entries.isLoading();
            progress.setVisible(loading);
            parent.setDisable(loading);
        });

        // start by loading entries of current month
        model.hour_entries.loadMonth(model.getMonth());
    }


}
