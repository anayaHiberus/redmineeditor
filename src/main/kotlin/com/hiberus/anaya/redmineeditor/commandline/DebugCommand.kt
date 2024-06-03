package com.hiberus.anaya.redmineeditor.commandline

import com.hiberus.anaya.redmineeditor.utils.IS_DEBUG
import javafx.application.Application

/** Command to set the [IS_DEBUG] parameter to true */
class DebugCommand : Command {
    override val name = "Debug messages"
    override val argument = "-debug"
    override val skipUI = false // don't skip

    override val help = listOf("For debugging purposes: Specify this flag to print debug messages.")

    override fun run(parameters: Application.Parameters) {
        IS_DEBUG = true
        println("IS_DEBUG set to true, debug messages will be shown")
    }
}