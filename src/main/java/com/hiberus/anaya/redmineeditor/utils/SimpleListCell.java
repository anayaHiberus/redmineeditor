package com.hiberus.anaya.redmineeditor.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

import java.io.IOException;

/**
 * A ListCell that loads its data from a fxml file
 * Adapted from https://stackoverflow.com/a/47526952
 *
 * @param <T> type of data for this cell
 */
public abstract class SimpleListCell<T> extends ListCell<T> {

    /**
     * Creates a new cell from a fxml file
     *
     * @param resource the fxml file to load
     */
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
            // empty cell
            setText(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            // cell with content
            update();
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }

    /**
     * Update here the content of the cell
     */
    protected abstract void update();
}
