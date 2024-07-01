package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineeditor.HEADLESS
import com.hiberus.anaya.redmineeditor.dialogs.MyException
import com.hiberus.anaya.redmineeditor.dialogs.ShowSettingsDialog
import com.hiberus.anaya.redmineeditor.dialogs.convert
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType

// - Nooooo you need a whole 100TB of frameworks to use beans
// - haha, static go brrrrr
val AppController = Controller()

/** The main application controller */
class Controller {

    /* ------------------------- model management ------------------------- */

    /** the model used */
    private val model = Model.Editor()

    /**
     * Run something in foreground.
     * Call this to have access to a readonly version of the model
     *
     * @param foreground what to run
     */
    fun <T> runForeground(foreground: (Model) -> T) = foreground(model)

    /**
     * Run something in background, and then something in foreground. Updates the loading status.
     * Call this to have access to a readWrite version of the model.
     *
     * @param background to run in background while the loading indicator is shown
     */
    fun runBackground(background: (Model.Editor) -> Unit) = runBackground(background) {}

    /**
     * Run something in background, and then something in foreground. Updates the loading status.
     * Call this to have access to a readWrite version of the model.
     *
     * @param background to run in background while the loading indicator is shown
     * @param later      to run in foreground after the background thread finishes (the parameter indicates if the background code was successful, if false it means there was an error). Optional
     */
    fun runBackground(background: (Model.Editor) -> Unit, later: (Boolean) -> Unit = {}) {
        // set as loading
        model.isLoading = true
        fireChanges()

        daemonThread {
            val error = runCatching {
                // run in background
                background(model)
            }.exceptionOrNull()?.convert {
                // background error
                MyException("Internal error", "Something unexpected happened in background")
            }

            // unset as loading
            model.isLoading = false
            fireChanges()

            // then in foreground (or not if headless)
            ({
                // show error
                error?.showAndWait()
                // notify for later
                later(error == null)
            }).let { if (HEADLESS) it() else Platform.runLater(it) }
        }
    }

    /* ------------------------- changes management ------------------------- */

    /** saved list of listeners and its data */
    private val listeners = mutableListOf<Pair<Set<ChangeEvent>, (Model) -> Unit>>()

    /**
     * Registers a new listener that will react to events.
     * Listeners will be called in register order
     *
     * @param events   list of events to react to
     * @param listener listener
     */
    fun onChanges(events: Set<ChangeEvent>, listener: (Model) -> Unit) =
        listeners.add(events to listener)

    /**
     * Fires changes made to the model (or specific ones)
     *
     * @param events events to fire, those from the model by default
     */
    fun fireChanges(events: Set<ChangeEvent> = model.getChanges()) =
        listeners.also { debugln("Changes: $events") } // debug
            // get those who need to be modified
            .filter { (lEvents, _) -> lEvents intersects events }
            // and notify them all in foreground
            .let {
                if (it.isNotEmpty()) {
                    runInForeground { // TODO: consider adding a try/catch here
                        it.forEach { (_, listener) -> listener(model) }
                    }
                }
            }

    /* ------------------------- common methods ------------------------- */

    /**
     * reloads the model data
     * asks first if there are changes that may be lost (unless [askIfChanges] is false)
     * Also resets the day if [resetDay] is set
     */
    fun reload(askIfChanges: Boolean = true, resetDay: Boolean = false) {

        // ask first
        if (askIfChanges && model.hasChanges && !confirmLoseChanges("reload")) return

        // init
        val uninitializedSettings = AppSettings.URL.value.isBlank() || AppSettings.KEY.value.isBlank()
        var calendarERROR: String? = null
        var colorsERROR: String? = null
        runBackground({ model ->

            // reload files
            calendarERROR = LoadCalendar()
            colorsERROR = LoadColors()

            // set now
            if (resetDay) model.toNow()

            // reload data
            // TODO: don't reload when uploading, update internal state
            model.reloadRedmine(clearOnly = uninitializedSettings)

        }) {
            // after loading
            if (uninitializedSettings) {
                // invalid configuration, ask to configure
                Alert(Alert.AlertType.CONFIRMATION).apply {
                    title = "Missing configuration"
                    contentText = "No valid configuration found, do you want to open settings?"
                    stylize()
                    addButton(ButtonType.OK) { ShowSettingsDialog() }
                }.show() // don't use showAndWait, seems to fail on first launch for some reason (the settings screen is empty)
            } else {
                if (calendarERROR != null) {
                    // invalid calendar, warning
                    Alert(Alert.AlertType.WARNING).apply {
                        title = "Calendar error"
                        contentText = "There were some issues reading the calendar file, some days may have invalid required hours:\n\n$calendarERROR"
                        stylize()
                    }.showAndWait()
                }
                if (colorsERROR != null) {
                    // invalid colors, warning
                    Alert(Alert.AlertType.WARNING).apply {
                        title = "Colors error"
                        contentText = "There were some issues reading the colors file, some colors may not be shown correctly:\n\n$colorsERROR"
                        stylize()
                    }.showAndWait()
                }
            }
        }
    }

}

/* ------------------------- public utils ------------------------- */

/** Settings that will trigger a reload */
val ReloadSettings = setOf(AppSettings.URL, AppSettings.KEY, AppSettings.PREV_DAYS, AppSettings.SCHEDULE_FILE, AppSettings.IGNORE_OLD_ASSIGNED, AppSettings.IGNORE_SSL_ERRORS)
