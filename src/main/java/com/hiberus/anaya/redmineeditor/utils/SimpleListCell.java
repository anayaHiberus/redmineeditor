package com.hiberus.anaya.redmineeditor.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

import java.io.IOException;

public abstract class SimpleListCell<T> extends ListCell<T> {

    public SimpleListCell(String resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            loader.setController(this);
            loader.setRoot(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            update();
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }

    protected abstract void update();
}
