package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Model;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * View for the parent app. Manages the loading indicator
 * [Also includes the subcontroller model injection]
 */
public class ParentView extends InnerView {


    /* ------------------------- subcontroller model injection ------------------------- */
    // TODO: replace with a bean or something

    @FXML
    private CalendarView calendarController;
    @FXML
    private SummaryView summaryController;
    @FXML
    private EntriesView entriesController;
    @FXML
    private InsertView insertController;
    @FXML
    private ActionsView actionsController;

    @FXML
    private void initialize() {
        // init subcontrollers, like manual dependency injection
        Model model = new Model();
        calendarController.injectModel(model);
        summaryController.injectModel(model);
        entriesController.injectModel(model);
        insertController.injectModel(model);
        actionsController.injectModel(model);
        this.injectModel(model); // must be last!

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
        model.notificator.register(Set.of(Model.Events.Loading), () -> {
            progress.setVisible(model.isLoading());
            parent.setDisable(model.isLoading());
        });


        // start by loading current day and month
        model.setMonth(YearMonth.now());
        model.setDay(LocalDate.now().getDayOfMonth());
        inBackground(model::loadMonth);
    }

}
