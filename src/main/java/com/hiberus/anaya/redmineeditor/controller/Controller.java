package com.hiberus.anaya.redmineeditor.controller;

import com.hiberus.anaya.redmineeditor.model.ChangeEvents;
import com.hiberus.anaya.redmineeditor.model.Model;
import com.hiberus.anaya.redmineeditor.utils.FXUtils;
import javafx.application.Platform;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The main application controller
 */
public class Controller {
    private final Model.Editor model = new Model.Editor(); // the model used

    private final List<Pair<Set<ChangeEvents>, ReadOnly>> listeners = new ArrayList<>(); // saved list of listeners and its data

    /**
     * A Runnable readonly model code
     * For foreground
     */
    public interface ReadOnly {
        /**
         * @param model readonly model
         */
        void run(Model model);
    }

    /**
     * A runnable for readwrite model code
     * For background
     */
    public interface ReadWrite {
        /**
         * @param model readwrite model
         * @throws MyException something went wrong
         */
        void run(Model.Editor model) throws MyException;
    }

    /**
     * Registers a new listener that will react to events.
     * Listeners will be called in register order
     *
     * @param events   list of events to react to
     * @param listener listener
     */
    public void register(Set<ChangeEvents> events, ReadOnly listener) {
        listeners.add(new Pair<>(events, listener));
    }

    /**
     * Run something in foreground.
     * Call this to have access to a readonly version of the model
     *
     * @param foreground what to run
     */
    public void runForeground(ReadOnly foreground) {
        foreground.run(model);
    }

    /**
     * Run something in background. Updates the loading status.
     * Call this to have access to a readWrite version of the model.
     *
     * @param background to run in background while the loading indicator is shown
     */
    public void runBackground(ReadWrite background) {
        runBackground(background, null);
    }

    /**
     * Run something in background, and then something in foreground. Updates the loading status.
     * Call this to have access to a readWrite version of the model.
     *
     * @param background to run in background while the loading indicator is shown
     * @param later      to run in foreground after the background thread finishes (the parameter indicates if there was an error)
     */
    public void runBackground(ReadWrite background, Consumer<Boolean> later) {
        // set as loading
        model.setLoading(true);
        fireChanges();

        // error container
        MyException[] error = new MyException[1];

        new Thread(() -> {
            try {
                // run in background
                background.run(model);
            } catch (MyException e) {
                error[0] = e; // save directly
            } catch (Throwable e) {
                // background error
                error[0] = new MyException("Internal error", "Something unexpected happened in background", e);
            }

            // unset as loading
            model.setLoading(false);
            fireChanges();

            Platform.runLater(() -> {
                // show error
                if (error[0] != null) error[0].showAndWait();
                // notify for later
                if (later != null) later.accept(error[0] == null);
            });
        }).start();
    }

    /**
     * Fires changes made to the model
     */
    public void fireChanges() {
        fireChanges(model.getChanges());
    }

    /**
     * Fires specific events
     *
     * @param events events to fire
     */
    public void fireChanges(Set<ChangeEvents> events) {
        listeners.forEach(pair -> {
            if (intersects(events, pair.getKey())) {
                // at least a registered event, run in foreground
                FXUtils.runInForeground(() -> pair.getValue().run(model));
            }
        });
    }

    /* ------------------------- utils ------------------------- */

    private static <E> boolean intersects(Set<E> a, Set<E> b) {
        // test if there is at least a common element in both sets
        HashSet<E> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return !intersection.isEmpty();
    }
}
