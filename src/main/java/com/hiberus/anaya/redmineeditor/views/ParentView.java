package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * View for the parent app. Manages the loading indicator
 */
public class ParentView {


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

    // ------------------------- elements -------------------------

    @FXML
    ProgressIndicator progress;

    @FXML
    VBox parent;

    // ------------------------- init -------------------------

    @FXML
    void initialize() {
        // this is the application controller
        Controller controller = new Controller(calendarController, summaryController, actionsController, entriesController, this);

        // init subviews, like manual dependency injection
        calendarController.injectController(controller);
        summaryController.injectController(controller);
        actionsController.injectController(controller);
        entriesController.injectController(controller);

        // start
        controller.start();
    }

    // ------------------------- actions -------------------------

    public void setLoading(boolean loading) {
        progress.setVisible(loading);
        parent.setDisable(loading);
    }

}
