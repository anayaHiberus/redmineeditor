package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.Controller
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import javafx.fxml.FXML
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.VBox
import java.time.LocalDate
import java.time.YearMonth

/**
 * View for the parent app. Manages the loading indicator
 * [Also includes the components bean injection]
 */
internal class ParentComponent : BaseComponent() {

    /* ------------------------- component injection ------------------------- */

    // javaFX calls them controllers, but they are components!
    // TODO: replace with a bean or something

    @FXML
    lateinit var calendarController: CalendarComponent

    @FXML
    lateinit var summaryController: SummaryComponent

    @FXML
    lateinit var entriesController: EntriesComponent

    @FXML
    lateinit var insertController: InsertComponent

    @FXML
    lateinit var actionsController: ActionsComponent

    @FXML
    private fun initialize() {
        // init components, like manual dependency injection
        Controller().let {
            calendarController.injectController(it)
            summaryController.injectController(it)
            entriesController.injectController(it)
            insertController.injectController(it)
            actionsController.injectController(it)
            this.injectController(it) // must be last!
        }
    }


    /* ****************************************************** */
    /* ****************************************************** */
    /* ****************************************************** */


    /* ------------------------- elements ------------------------- */

    @FXML
    lateinit var progress: ProgressIndicator  // that circular thingy

    @FXML
    lateinit var parent: VBox // the main app, disabled while loading

    /* ------------------------- init ------------------------- */

    override fun init() {
        // When loading, the indicator is shown and the whole app is disabled.
        controller.onChanges(setOf(ChangeEvents.Loading)) {
            progress.isVisible = it.isLoading
            parent.isDisable = it.isLoading
        }


        // start by loading current day and month
        controller.runBackground {
            it.month = YearMonth.now()
            it.day = LocalDate.now().dayOfMonth
            controller.fireChanges() // notify now to display month while loading
            it.loadMonth()
        }
    }
}