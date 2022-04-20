package com.hiberus.anaya.redmineeditor.commandline

import com.hiberus.anaya.redmineapi.READ_ONLY
import javafx.application.Application

/**
 * Command to set the [READ_ONLY] parameter to true
 */
class ReadOnlyCommand : Command {
    override val name = "Read only flag"
    override val argument = "-readOnly"
    override val skipUI = false // don't skip

    override fun showHelp() = println("For testing purposes: Specify this flag to disable all put/post petitions, they will be skipped (but still logged)")

    override fun run(parameters: Application.Parameters) {
        READ_ONLY = true
        println("READ_ONLY set to true, all put/post petitions will be skipped (but still logged)")
    }
}