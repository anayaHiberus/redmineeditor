package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Model;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * View for the parent app. Manages the loading indicator
 */
public class ParentView {

    // ------------------------- model -------------------------

    private final Model model = new Model(); // this is the application model object

    // ------------------------- subviews -------------------------

    // 'controller' is the wrong name by fxml
    @FXML
    CalendarView calendarController;
    @FXML
    SummaryView summaryController;
    @FXML
    ActionsView actionsController;
    @FXML
    EntriesView entriesController;

    // ------------------------- views -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

    // ------------------------- init -------------------------

    @FXML
    void initialize() {
        // init subviews, like manual dependency injection
        calendarController.injectModel(model);
        summaryController.injectModel(model);
        actionsController.injectModel(model);
        entriesController.injectModel(model);

        model.onChanges(() -> {
            // when entries are loading, show indicator and disable the rest
            boolean loading = model.time_entries.isLoading();
            progress.setVisible(loading);
            parent.setDisable(loading);
        });

        // start by loading entries of current month
        model.time_entries.loadMonth(model.getMonth());
    }


}
