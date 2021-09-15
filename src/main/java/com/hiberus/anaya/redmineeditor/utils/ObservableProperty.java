package com.hiberus.anaya.redmineeditor.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * A custom property with data that will notify others when its value changes
 *
 * @param <T> Type of the data
 */
public class ObservableProperty<T> {

    private T data; // the data
    private final Set<OnChangedListener<T>> listeners = new HashSet<>(); // list of listeners

    /**
     * Wraps data into an observable property.
     * If the data is of type Data, it is initialized too
     *
     * @param data data to wrap
     */
    public ObservableProperty(T data) {
        this.data = data;
        if (data instanceof ObservableProperty.Property) {
            ((Property) data).property = this;
        }
    }

    /**
     * @return the data value
     */
    public T get() {
        return data;
    }

    /**
     * Changes this property data. All listeners will be called
     *
     * @param value new value
     */
    public void set(T value) {
        data = value;
        notifyExcept(null);
    }

    /**
     * Registers a listener that will be notified when this property changes.
     * The listener will be called now
     *
     * @param listener listener to register
     * @return the observed property
     */
    public ObservedProperty observeAndNotify(OnChangedListener<T> listener) {
        listener.onChanged(get());
        return new ObservedProperty(listener);
    }

    /**
     * Registers a listener that will be notified when this property changes.
     * The listener will NOT be called now
     *
     * @param listener listener to register
     * @return the observed property
     */
    public ObservedProperty observe(OnChangedListener<T> listener) {
        return new ObservedProperty(listener);
    }

    private void notifyExcept(OnChangedListener<T> dontNotify) {
        // notify all listeners except required
        for (OnChangedListener<T> listener : listeners) {
            if (listener != dontNotify) listener.onChanged(data);
        }
    }

    // ------------------------- listener -------------------------

    /**
     * The listener that will be notified on Property changes
     *
     * @param <T>
     */
    public interface OnChangedListener<T> {
        /**
         * @param newValue is now the value of the property
         */
        void onChanged(T newValue);
    }

    // ------------------------- observed -------------------------

    /**
     * An observer for an existing property.
     * Similar as {@link ObservableProperty} but won't notify own listener when set
     */
    public class ObservedProperty {

        private final OnChangedListener<T> listener; // our listener

        private ObservedProperty(OnChangedListener<T> listener) {
            // save and register
            this.listener = listener;
            listeners.add(listener);
        }

        /**
         * @return this property data
         */
        public T get() {
            return data;
        }

        /**
         * Changes this property data. All listeners EXCEPT THIS ONE will be called
         *
         * @param value new value
         */
        public void set(T value) {
            data = value;
            notifyExcept(listener);
        }

        /**
         * Changes this property data. All listeners will be called
         *
         * @param value new value
         */
        public void setAndNotify(T value) {
            data = value;
            notifyExcept(null);
        }
    }

    // ------------------------- own manager -------------------------

    /**
     * A Property, in case you need to notify changes without changing its value
     */
    public static class Property {

        private ObservableProperty<?> property; // the own observable property, initialized when this Property is registered in an ObservableProperty

        /**
         * Trigger listeners without replacing the property object
         */
        public void notifyChanged() {
            property.notifyExcept(null);
        }
    }
}
