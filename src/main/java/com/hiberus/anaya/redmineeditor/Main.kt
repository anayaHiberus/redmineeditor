package com.hiberus.anaya.redmineeditor

import com.hiberus.anaya.redmineapi.READ_ONLY
import com.hiberus.anaya.redmineeditor.dialogs.FixRunToolCommandLine
import com.hiberus.anaya.redmineeditor.dialogs.TITLE
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * Main app
 */
class Main : Application() {

    /**
     * Starts the app process
     */
    override fun init() {

        // read only
        READ_ONLY = "-read_only" in parameters.unnamed

        if ("-h" in parameters.raw) {
            println("This is a test, it works!")
            Platform.exit()
        }

        if ("-fix" in parameters.raw) {
            FixRunToolCommandLine(parameters)
            Platform.exit()
        }
    }


    /**
     * Starts the app window (stage)
     */
    override fun start(stage: Stage) = stage.run {
        // load and show
        title = TITLE
        scene = Scene(FXMLLoader(ResourceLayout("parent")).load())
        scene.stylize()
        show()
    }

}