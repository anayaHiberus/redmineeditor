package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;

/**
 * Just an interface for inner controllers, so that they can be populated with the model from the parent (instead of using dependency inyection)
 */
public abstract class InnerCtrl {

    protected Model model;

    protected void injectModel(Model model) {
        this.model = model;
        initCtrl();
    }

    /**
     * Initializes this inner controller with the model object
     *
     * @param model application model object
     */
    public abstract void initCtrl();
}
