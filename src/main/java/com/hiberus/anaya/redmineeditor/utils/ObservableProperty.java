package com.hiberus.anaya.redmineeditor.utils;

import java.util.HashSet;
import java.util.Set;

public class ObservableProperty<T> {

    private T data;
    private final Set<OnChangedListener<T>> listeners = new HashSet<>();

    public ObservableProperty(T data) {
        this.data = data;
        if(data instanceof Data){
            ((Data) data).property = this;
        }
    }

    public T get() {
        return data;
    }

    public void set(T value) {
        data = value;
        notifyExcept(null);
    }

    public ObservedProperty observeAndNotify(OnChangedListener<T> listener){
        listener.onChanged(get());
        return new ObservedProperty(listener);
    }

    public ObservedProperty observe(OnChangedListener<T> listener){
        return new ObservedProperty(listener);
    }

    private void notifyExcept(OnChangedListener<T> dontNotify) {
        for (OnChangedListener<T> listener : listeners) {
            if(listener!=dontNotify) listener.onChanged(data);
        }
    }

    // ------------------------- listener -------------------------

    public interface OnChangedListener<T> {
        void onChanged(T newValue);
    }

    public class ObservedProperty {

        private final OnChangedListener<T> listener;

        public ObservedProperty(OnChangedListener<T> listener) {
            this.listener = listener;
            listeners.add(listener);
        }

        public T get() {
            return data;
        }

        public void set(T value) {
            data = value;
            notifyExcept(listener);
        }

        public void setAndNotify(T value){
            data = value;
            notifyExcept(null);
        }
    }

    // ------------------------- own manager -------------------------

    public static class Data {

        private ObservableProperty<?> property;

        public void notifyChanged(){
            property.notifyExcept(null);
        }
    }
}
