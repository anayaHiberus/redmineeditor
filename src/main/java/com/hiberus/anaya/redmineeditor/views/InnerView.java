package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Model;

/**
 * Just an interface for inner views, so that they can be populated with the model from the parent (instead of using dependency injection)
 */
public abstract class InnerView {

    protected Model model;

    /**
     * Initializes this inner view with the model object
     *
     * @param model application model object
     */
    protected void injectModel(Model model) {
        this.model = model;
        initView();
    }

    public abstract void initView();
}
