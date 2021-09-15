package com.hiberus.anaya.redmineeditor.utils;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;

public class IntelligentProperty<T> {

    public interface OnChangedListener<T> {
        void onChanged(T newValue);
    }

    private final ChangeListener<T> wrapperListener;
    private final Property<T> property;

    public IntelligentProperty(Property<T> property, OnChangedListener<T> listener) {
        this.property = property;
        wrapperListener = (observable, oldValue, newValue) -> listener.onChanged(newValue);
        this.property.addListener(wrapperListener);
        listener.onChanged(property.getValue());
    }

    public T get() {
        return property.getValue();
    }

    public void set(T value) {
        property.removeListener(wrapperListener);
        property.setValue(value);
        property.addListener(wrapperListener);
    }
}
