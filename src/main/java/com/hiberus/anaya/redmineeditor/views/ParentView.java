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
    private CalendarView calendarController;
    @FXML
    private SummaryView summaryController;
    @FXML
    private ActionsView actionsController;
    @FXML
    private EntriesView entriesController;

    // ------------------------- elements -------------------------

    @FXML
    private ProgressIndicator progress; // that circular thingy

    @FXML
    private VBox parent; // the main app, disabled while loading

    // ------------------------- init -------------------------

    @FXML
    private void initialize() {
        // this is the application controller
        Controller controller = new Controller(calendarController, summaryController, entriesController, this);

        // init subviews, like manual dependency injection
        calendarController.injectController(controller);
        summaryController.injectController(controller);
        actionsController.injectController(controller);
        entriesController.injectController(controller);

        // start
        controller.onStart();
    }

    // ------------------------- actions -------------------------

    /**
     * When loading, the indicator is shown and the whole app is disabled.
     * Make sure to enable it again!
     *
     * @param loading the loading state
     */
    public void setLoading(boolean loading) {
        progress.setVisible(loading);
        parent.setDisable(loading);
    }

}