package com.hiberus.anaya.redmineeditor.utils;

import javafx.beans.value.ObservableValue;

public class SimpleChangeListener {
    static public <T extends ObservableValue<P>, P> T register(T property, ISimpleChangeListener<P> listener) {
        listener.onValueChange(property.getValue());
        return registerSilently(property, listener);
    }

    static public <T extends ObservableValue<P>, P> T registerSilently(T property, ISimpleChangeListener<P> listener) {
        property.addListener((observable, oldValue, newValue) -> listener.onValueChange(newValue));
        return property;
    }

    public interface ISimpleChangeListener<P> {
        void onValueChange(P newValue);
    }
}
