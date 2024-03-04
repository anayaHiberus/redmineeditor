package com.hiberus.anaya.redmineeditor.utils

import javafx.fxml.FXMLLoader
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ListCell
import java.net.URL


/**
 * A ListCell that loads its data from a fxml file
 * Adapted from https://stackoverflow.com/a/47526952
 *
 * @param fxml the fxml file to load (from the module root)
 * @param <T> type of data for this cell
 */
abstract class SimpleListCell<T>(fxml: URL) : ListCell<T>() {

    init {
        // initialize ListCell by loading the fxml file
        FXMLLoader(fxml).apply {
            setController(this@SimpleListCell)
            setRoot(this@SimpleListCell)
            load()
        }
    }

    public override fun updateItem(item: T, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            // empty cell
            text = null
            contentDisplay = ContentDisplay.TEXT_ONLY
            style = null // clear style
        } else {
            // cell with content
            update()
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
        }
    }

    /**
     * Update here the content of the cell
     */
    protected abstract fun update()

}
