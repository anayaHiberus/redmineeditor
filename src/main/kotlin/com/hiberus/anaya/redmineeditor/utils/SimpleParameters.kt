package com.hiberus.anaya.redmineeditor.utils

import javafx.application.Application

/** A Parameters implementation that doesn't require JavaFX to be running. */
class SimpleParameters(private val args: Array<String>) : Application.Parameters() {
    override fun getRaw() = args.toList()

    override fun getUnnamed() = args.filter { !it.startsWith("--") }.filter { it.startsWith("-") }//TODO: remove prefix

    override fun getNamed() = args.filter { it.startsWith("--") }.map { it.removePrefix("--").split(Regex("="), 2)  }.associate { (l, r) -> l to r }
}