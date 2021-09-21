package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Controller;

/**
 * Just an interface for inner views, so that they can be populated with the model from the parent (instead of using dependency injection)
 */
abstract class InnerView {

    /**
     * The global controller
     */
    Controller controller;

    /**
     * Initializes this inner view with the global controller
     *
     * @param controller global controller
     */
    void injectController(Controller controller) {
        this.controller = controller;
    }

}
