package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Controller;

/**
 * Just an interface for inner views, so that they can be populated with the model from the parent (instead of using dependency injection)
 */
public abstract class InnerView {

    protected Controller controller;

    /**
     * Initializes this inner view with the model object
     *
     * @param model application model object
     */
    protected void injectController(Controller model) {
        this.controller = model;
        initView();
    }

    public abstract void initView();
}
