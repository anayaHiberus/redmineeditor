package com.hiberus.anaya.redmineeditor

import com.hiberus.anaya.redmineeditor.commandline.COMMANDS
import com.hiberus.anaya.redmineeditor.dialogs.TITLE
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * If this is specified, the help is shown and nothing else runs
 */
const val HELP_FLAG = "-h"

/**
 * Main app
 */
class Main : Application() {

    /**
     * Starts the app process
     */
    override fun init() {

        // help flag
        if (HELP_FLAG in parameters.raw) {
            // show all help
            println("These are the available commands:")
            COMMANDS.forEach {
                println()
                println("${it.name}: call with ${it.argument}${if (it.skipUI) " [will not run the UI afterwards]" else ""}")
                it.showHelp()
            }
            // and exit
            Platform.exit()
            return
        }

        // run valid commands
        COMMANDS.filter { it.argument in parameters.raw }
            .onEach {
                println("Running ${it.name} (${it.argument} found)${if (it.skipUI) " [the UI won't be shown afterwards]" else ""}:")
                it.run(parameters)
                println()
            }.run {
                if (any { it.skipUI }) {
                    // and exit if required
                    Platform.exit()
                    return
                }
            }

        // launch UI
        println("Launching UI (run with $HELP_FLAG to see a list of commands)")
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