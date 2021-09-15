package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;

/**
 * Just an interface for inner controllers, so that they can be populated with the model from the parent (instead of using dependency inyection)
 */
public interface InnerCtrl {
    /**
     * Initializes this inner controller with the model object
     *
     * @param model application model object
     */
    void initCtrl(Model model);
}
