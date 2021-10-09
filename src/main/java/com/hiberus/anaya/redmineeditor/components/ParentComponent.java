package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineeditor.controller.Controller;
import com.hiberus.anaya.redmineeditor.model.ChangeEvents;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * View for the parent app. Manages the loading indicator
 * [Also includes the components injection]
 */
public class ParentComponent extends BaseComponent {


    /* ------------------------- component injection ------------------------- */
    // javaFX calls them controllers, but they are components!
    // TODO: replace with a bean or something

    @FXML
    private CalendarComponent calendarController;
    @FXML
    private SummaryComponent summaryController;
    @FXML
    private EntriesComponent entriesController;
    @FXML
    private InsertComponent insertController;
    @FXML
    private ActionsComponent actionsController;

    @FXML
    private void initialize() {
        // init components, like manual dependency injection
        Controller controller = new Controller();
        calendarController.injectController(controller);
        summaryController.injectController(controller);
        entriesController.injectController(controller);
        insertController.injectController(controller);
        actionsController.injectController(controller);
        this.injectController(controller); // must be last!
    }

    /* ****************************************************** */
    /* ****************************************************** */
    /* ****************************************************** */

    /* ------------------------- elements ------------------------- */

    @FXML
    private ProgressIndicator progress; // that circular thingy

    @FXML
    private VBox parent; // the main app, disabled while loading

    /* ------------------------- init ------------------------- */

    @Override
    void init() {
        // When loading, the indicator is shown and the whole app is disabled.
        controller.register(Set.of(ChangeEvents.Loading), model -> {
            progress.setVisible(model.isLoading());
            parent.setDisable(model.isLoading());
        });


        // start by loading current day and month
        controller.runBackground(model -> {
            model.setMonth(YearMonth.now());
            model.setDay(LocalDate.now().getDayOfMonth());
            controller.fireChanges();
            model.loadMonth();
        });
    }

}
