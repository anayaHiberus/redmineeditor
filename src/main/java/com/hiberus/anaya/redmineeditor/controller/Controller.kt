package com.hiberus.anaya.redmineeditor.controller

import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.FXUtils.runInForeground
import javafx.application.Platform
import kotlin.concurrent.thread

/**
 * The main application controller
 */
class Controller {

    /* ------------------------- data ------------------------- */

    private val model = Model.Editor() // the model used

    private val listeners: MutableList<Pair<Set<ChangeEvents>, (Model) -> Unit>> = mutableListOf() // saved list of listeners and its data

    /* ------------------------- methods ------------------------- */

    /**
     * Registers a new listener that will react to events.
     * Listeners will be called in register order
     *
     * @param events   list of events to react to
     * @param listener listener
     */
    fun register(events: Set<ChangeEvents>, listener: (Model) -> Unit) =
        listeners.add(events to listener).run { }

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

        thread {
            val error = try {
                // run in background
                background(model)
                null
            } catch (e: MyException) {
                e // save directly
            } catch (e: Throwable) {
                // background error
                MyException("Internal error", "Something unexpected happened in background", e)
            }

            // unset as loading
            model.isLoading = false
            fireChanges()

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
    @JvmOverloads
    fun fireChanges(events: Set<ChangeEvents> = model.changes) =
        listeners.forEach { (lEvents, listener) ->
            // if at least a registered event, run in foreground
            if (events intersects lEvents) runInForeground { listener(model) }
        }

}

/* ------------------------- utils ------------------------- */

/**
 * test if there is at least a common element in both sets
 */
private infix fun <E> Set<E>.intersects(other: Set<E>) =
    (this intersect other).isNotEmpty()
