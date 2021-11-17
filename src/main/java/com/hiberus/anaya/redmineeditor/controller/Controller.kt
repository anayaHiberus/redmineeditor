package com.hiberus.anaya.redmineeditor.controller

import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.settings.AppSettings
import com.hiberus.anaya.redmineeditor.utils.hiberus.LoadSpecialDays
import com.hiberus.anaya.redmineeditor.utils.runInForeground
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.stage.Window
import kotlin.concurrent.thread

// - Nooooo you need a whole 100TB of frameworks to use beans
// - haha, static go brrrrr
val AppController = Controller()

/**
 * The main application controller
 */
class Controller {

    /* ------------------------- data ------------------------- */

    /**
     * the model used
     */
    private val model = Model.Editor()

    /**
     * saved list of listeners and its data
     */
    private val listeners = mutableListOf<Pair<Set<ChangeEvents>, (Model) -> Unit>>()

    /* ------------------------- methods ------------------------- */

    /**
     * Registers a new listener that will react to events.
     * Listeners will be called in register order
     *
     * @param events   list of events to react to
     * @param listener listener
     */
    fun onChanges(events: Set<ChangeEvents>, listener: (Model) -> Unit) =
        listeners.add(events to listener)

    /**
     * Run something in foreground.
     * Call this to have access to a readonly version of the model
     *
     * @param foreground what to run
     */
    fun runForeground(foreground: (Model) -> Unit) = foreground(model)


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
     * @param later      to run in foreground after the background thread finishes (the parameter indicates if there was an error). Optional
     */
    fun runBackground(background: (Model.Editor) -> Unit, later: (Boolean) -> Unit = {}) {
        // set as loading
        model.isLoading = true
        fireChanges()

        thread(isDaemon = true) {
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

            // then in foreground
            Platform.runLater {
                // show error
                error?.showAndWait()
                // notify for later
                later(error == null)
            }
        }
    }

    /**
     * Fires changes made to the model (or specific ones)
     *
     * @param events events to fire, those from the model by default
     */
    fun fireChanges(events: Set<ChangeEvents> = model.getChanges()) =
        listeners.also { println("Changes: $events") } // debug
            // get those who need to be modified
            .filter { (lEvents, _) -> lEvents intersects events }
            // and notify them all in foreground
            .let {
                runInForeground { // TODO: consider adding a try/catch here
                    it.forEach { (_, listener) -> listener(model) }
                }
            }

    /**
     * reloads the model data (loses changes, if existing)
     * Also reloads configuration if [reloadConfig] is set
     * Also resets the day if [resetDay] is set
     */
    fun reload(reloadConfig: Boolean = false, resetDay: Boolean = false) {
        var settingsERROR = false
        var specialDaysERROR = false
        runBackground({ model ->

            // reload files
            if (reloadConfig) {
                settingsERROR = !AppSettings.load()
                specialDaysERROR = !LoadSpecialDays()

                runInForeground {
                    // stylize displayed windows (should only be the main one)
                    Window.getWindows().map { it.scene }.distinct().forEach { it.stylize() }
                }
            }

            // set now
            if (resetDay) model.toNow()

            // reload data
            // TODO: don't reload when uploading, update internal state
            model.reloadRedmine(clearOnly = settingsERROR)

            // notify so that the ui is updated at this step and everything is updated
            AppController.fireChanges()

            // load month
            model.loadDate()

        }) {
            // after loading
            if (settingsERROR) {
                // invalid configuration, error
                Alert(Alert.AlertType.ERROR).apply {
                    title = "Configuration error"
                    contentText = "No valid configuration found"
                    stylize()
                }.showAndWait()
            }
            if (specialDaysERROR) {
                // invalid special days, warning
                Alert(Alert.AlertType.WARNING).apply {
                    title = "Special days error"
                    contentText = "No valid special days data found"
                    stylize()
                }.showAndWait()
            }
        }
    }

}

/* ------------------------- utils ------------------------- */

/**
 * test if there is at least a common element in both sets
 */
private infix fun <E> Set<E>.intersects(other: Set<E>) =
    (this intersect other).isNotEmpty()
