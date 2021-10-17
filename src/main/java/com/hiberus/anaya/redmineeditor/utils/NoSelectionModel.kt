package com.hiberus.anaya.redmineeditor.utils

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.MultipleSelectionModel

/**
 * Try to avoid selection in the custom listview
 * From https://stackoverflow.com/a/46186195
 * TODO: fix blue border on focus
 *
 * @param <T> Type of items
 */
class NoSelectionModel<T> : MultipleSelectionModel<T>() {
    override fun getSelectedIndices(): ObservableList<Int> = FXCollections.emptyObservableList()
    override fun getSelectedItems(): ObservableList<T> = FXCollections.emptyObservableList()
    override fun selectIndices(index: Int, vararg indices: Int) {}
    override fun selectAll() {}
    override fun selectFirst() {}
    override fun selectLast() {}
    override fun clearAndSelect(index: Int) {}
    override fun select(index: Int) {}
    override fun select(obj: T) {}
    override fun clearSelection(index: Int) {}
    override fun clearSelection() {}
    override fun isSelected(index: Int) = false
    override fun isEmpty() = true
    override fun selectPrevious() {}
    override fun selectNext() {}
}