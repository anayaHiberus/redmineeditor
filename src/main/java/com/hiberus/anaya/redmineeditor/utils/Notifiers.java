package com.hiberus.anaya.redmineeditor.utils;

import javafx.application.Platform;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Custom Notifier event system that allows to fire multiple events and notify later them all simultaneously
 *
 * @param <T> Enum for which events are valid
 */
public class Notifiers<T extends Enum<T>> {

    private final List<Pair<Set<T>, Runnable>> listeners = new ArrayList<>(); // saved list of listeners and its data
    private final Set<T> firedEvents = new HashSet<>(); // fired but still not delivered events

    /**
     * Registers a new listener that will react to events.
     * Listeners will be called in register order
     *
     * @param events   list of events to react to
     * @param listener listener
     */
    public void register(Set<T> events, Runnable listener) {
        listeners.add(new Pair<>(events, listener));
    }

    /**
     * Fires an event
     *
     * @param event event to fire
     */
    public void fire(T event) {
        // add to fired
        firedEvents.add(event);

        // schedule for later (using JavaFX, but could be changed)
        Platform.runLater(() -> {
            listeners.forEach(pair -> {
                if (intersects(firedEvents, pair.getKey())) {
                    // at least a registered event, run
                    pair.getValue().run();
                }
            });
            firedEvents.clear();
        });
    }

    private static <E> boolean intersects(Set<E> a, Set<E> b) {
        // test if there is at least a common element in both sets
        HashSet<E> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return !intersection.isEmpty();
    }

}