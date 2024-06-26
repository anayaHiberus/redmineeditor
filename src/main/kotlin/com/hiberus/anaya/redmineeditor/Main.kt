package com.hiberus.anaya.redmineeditor

import com.hiberus.anaya.redmineeditor.commandline.COMMANDS
import com.hiberus.anaya.redmineeditor.dialogs.TITLE
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.SimpleParameters
import com.hiberus.anaya.redmineeditor.utils.centerInMouseScreen
import com.hiberus.anaya.redmineeditor.utils.stylize
import com.hiberus.anaya.tools.IgnoreSSLErrors
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

/** iff we are running on headless mode (without a UI) */
var HEADLESS = true

/** If this is specified, the help is shown and nothing else runs */
const val HELP_FLAG = "-h"

/** Starts the app. */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            // SSL fix
            if (AppSettings.IGNORE_SSL_ERRORS.value.toBoolean()) IgnoreSSLErrors()


            // help flag
            if (HELP_FLAG in args) {
                // get requested commands
                COMMANDS.filter { it.argument in args }.ifEmpty {
                    // show all help
                    println("These are all the available commands:")
                    COMMANDS
                }.forEach { command ->
                    println()
                    println(buildString {
                        append(command.name)
                        append(": call as ")
                        append(command.argument)
                        if (command.parameters.isNotBlank()) append(" ").append(command.parameters)
                        if (command.skipUI) append(" {will not run the UI afterwards}")
                    })
                    println(command.help.joinToString("\n") { "    $it" })
                }
                // and exit
                return
            }

            // run valid commands
            COMMANDS.filter { it.argument in args }
                .onEach { command ->
                    println("Running ${command.name} (${command.argument} found)${if (command.skipUI) " [the UI won't be shown afterwards]" else ""}:")
                    command.run(SimpleParameters(args))
                    println()
                }.run {
                    if (any { it.skipUI }) {
                        // and exit if required
                        return
                    }
                }

            // launch UI
            println("Launching UI (run with $HELP_FLAG to see a list of commands)")
            HEADLESS = false
            Application.launch(App::class.java, *args)
        }
    }
}

/** Starts the window (stage) */
class App : Application() {
    override fun start(stage: Stage) = stage.run {
        // load and show
        title = TITLE
        scene = Scene(FXMLLoader(ResourceLayout("parent")).load())
        scene.stylize()
        centerInMouseScreen()
        show()
    }
}