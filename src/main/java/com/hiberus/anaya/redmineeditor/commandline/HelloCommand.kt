package com.hiberus.anaya.redmineeditor.commandline

import javafx.application.Application

/**
 * Sample command
 */
class HelloCommand : Command {
    override val name = "Hello World"
    override val argument = "-hello"
    override val help = listOf("Run this command to display a nice 'Hello World' message!")
    override fun run(parameters: Application.Parameters) = println("Hello World")
}