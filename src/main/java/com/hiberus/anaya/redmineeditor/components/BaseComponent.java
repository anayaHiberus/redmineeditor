package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineeditor.Controller;

/**
 * Just an interface for controllers, so that they can be populated with the model from the parent (instead of using dependency injection)
 */
abstract class BaseComponent {

    /* ------------------------- Model bean injection ------------------------- */

    /**
     * The controller field
     */
    Controller controller;

    /**
     * Initializes this inner view with the global model
     *
     * @param model global model
     */
    void injectController(Controller controller) {
        this.controller = controller;
        init();
    }

    /**
     * Initialization to do when the model is populated
     */
    abstract void init();

}
