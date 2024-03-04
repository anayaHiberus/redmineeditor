package com.hiberus.anaya.redmineeditor.commandline

import com.hiberus.anaya.redmineeditor.dialogs.CalendarStatisticsCommand
import com.hiberus.anaya.redmineeditor.dialogs.FixMonthToolCommand
import com.hiberus.anaya.redmineeditor.dialogs.SettingsCommand
import javafx.application.Application


/**
 * Registered commands
 */
val COMMANDS = listOf(
    SettingsCommand(),
    ReadOnlyCommand(),
    FixMonthToolCommand(),
    CalendarStatisticsCommand(),
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
     * Extra parameters that the command requires (for logging purposes)
     */
    val parameters: String
        get() = ""

    /**
     * If true will not show the UI afterward (true by default)
     */
    val skipUI: Boolean
        get() = true

    /**
     * help of this command. Each line will be displayed padded when the user specifies the help command line argument
     */
    val help: List<String>

    /**
     * Should run the command
     * Will be run when the user specified the [argument] command line argument
     */
    fun run(parameters: Application.Parameters)
}