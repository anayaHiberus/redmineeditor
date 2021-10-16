package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.Controller

/**
 * Just an interface for components, so that they can be populated with the controller from the parent (instead of using dependency injection)
 * TODO: add dependency injection
 */
internal abstract class BaseComponent {

    /* ------------------------- bean injection ------------------------- */

    /**
     * The controller field
     */
    lateinit var controller: Controller

    /**
     * Initializes this component with the global controller
     *
     * @param controller global controller
     */
    fun injectController(controller: Controller) {
        this.controller = controller
        init()
    }

    /**
     * Initialization to do when the controller is populated
     */
    abstract fun init()

}