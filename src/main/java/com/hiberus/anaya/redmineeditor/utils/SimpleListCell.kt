package com.hiberus.anaya.redmineeditor.utils

import javafx.fxml.FXMLLoader
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ListCell


/**
 * A ListCell that loads its data from a fxml file
 * Adapted from https://stackoverflow.com/a/47526952
 *
 * @param filename the fxml file to load (from the module root)
 * @param <T> type of data for this cell
 */
abstract class SimpleListCell<T>(filename: String) : ListCell<T>() {

    init {
        // initialize ListCell by loading the fxml file
        FXMLLoader(javaClass.getResource(
            "/${javaClass.modulePath}/$filename"
        )).apply {
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
            setContentDisplay(ContentDisplay.TEXT_ONLY)
        } else {
            // cell with content
            update()
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY)
        }
    }

    /**
     * Update here the content of the cell
     */
    protected abstract fun update()

}

/* ------------------------- utils ------------------------- */

/**
 * The module of the class, as path
 */
private val Class<*>.modulePath
    get() = module.name.replace(".", "/")
