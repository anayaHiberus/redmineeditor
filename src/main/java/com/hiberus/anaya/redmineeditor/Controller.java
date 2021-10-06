package com.hiberus.anaya.redmineeditor;

import javafx.application.Platform;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Controller {
    private final Model model = new Model(); // the model

    private final List<Pair<Set<Model.ModelEditor.Events>, ReadOnly>> listeners = new ArrayList<>(); // saved list of listeners and its data

    public interface ReadOnly {
        void run(Model model);
    }

    public interface ReadWrite {
        void run(Model.ModelEditor editor) throws MyException;
    }

    /**
     * Registers a new listener that will react to events.
     * Listeners will be called in register order
     *
     * @param events   list of events to react to
     * @param listener listener
     */
    public void register(Set<Model.ModelEditor.Events> events, ReadOnly listener) {
        listeners.add(new Pair<>(events, listener));
    }

    public void runForeground(ReadOnly runnable) {
        runnable.run(model);
    }

    /**
     * Run something in background, and then something in foreground. Updates the loading status
     *
     * @param background to run in background
     * @param foreground to run in foreground after the background thread finishes without errors
     */
    public void runBackground(ReadWrite background, Runnable later) {
        // set as loading
        Model.ModelEditor editableModel = model.edit();
        editableModel.setLoading(true);
        fireChanges(editableModel);

        // error container
        MyException[] error = new MyException[1];

        new Thread(() -> {
            try {
                // run in background
                background.run(editableModel);
            } catch (MyException e) {
                error[0] = e; // save directly
            } catch (Throwable e) {
                // background error
                error[0] = new MyException("Internal error", "Something unexpected happened in background", e);
            }

            // unset as loading
            editableModel.setLoading(false);
            fireChanges(editableModel);

            Platform.runLater(() -> {
                // show error
                if (error[0] != null) error[0].showAndWait();
                // notify for later
                if (later != null) later.run();
            });
        }).start();
    }

    /**
     * Fires an event
     *
     * @param event         event to fire
     * @param editableModel
     */
    public void fireChanges(Model.ModelEditor editableModel) {
        fireChanges(editableModel.changes);
        editableModel.changes.clear();
    }


    public void fireChanges(Set<Model.ModelEditor.Events> events) {
        listeners.forEach(pair -> {
            if (intersects(events, pair.getKey())) {
                // at least a registered event, run
                Platform.runLater(() -> pair.getValue().run(model));
            }
        });
    }


    private static <E> boolean intersects(Set<E> a, Set<E> b) {
        // test if there is at least a common element in both sets
        HashSet<E> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return !intersection.isEmpty();
    }
}
