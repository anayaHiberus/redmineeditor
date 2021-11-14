package com.hiberus.anaya.redmineeditor.controller

import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.runInForeground
import javafx.application.Platform
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

}

/* ------------------------- utils ------------------------- */

/**
 * test if there is at least a common element in both sets
 */
private infix fun <E> Set<E>.intersects(other: Set<E>) =
    (this intersect other).isNotEmpty()
