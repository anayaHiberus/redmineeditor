package com.hiberus.anaya.redmineeditor.commandline

import com.hiberus.anaya.redmineeditor.dialogs.FixMonthToolCommand
import javafx.application.Application


/**
 * Registered commands
 */
val COMMANDS = listOf(
    ReadOnlyCommand(),
    HelloCommand(),
    FixMonthToolCommand()
)

/**
 * Defines an available command of the app
 * Remember to register it above
 */
interface Command {

    /**
     * Name of the command
     */
    val name: String

    /**
     * Command line argument that should be present in order to run this command
     */
    val argument: String

    /**
     * If true will not show the UI afterwards (true by default)
     */
    val skipUI: Boolean
        get() = true

    /**
     * Should show the related help of this command in the standard output
     * Will be run when the user specifies the help command line argument
     */
    fun showHelp()

    /**
     * Should run the command
     * Will be run when the user specified the [argument] command line argument
     */
    fun run(parameters: Application.Parameters)
}