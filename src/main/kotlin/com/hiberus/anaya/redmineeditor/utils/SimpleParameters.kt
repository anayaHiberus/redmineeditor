package com.hiberus.anaya.redmineeditor.utils

import javafx.application.Application

/** A Parameters implementation that doesn't require JavaFX to be running. (also fixes inconsistency with unnamed) */
class SimpleParameters(private val args: Array<String>) : Application.Parameters() {
    override fun getRaw() = args.toList()

    /** Returns all unnamed parameters: "-a -bc --d=1 --ef=2" => ["a", "bc"] */
    override fun getUnnamed() = args.filter { it.startsWith("-") }.filterNot { it.startsWith("--") }.map { it.removePrefix("-") }

    /** Returns all named parameters: "-a -bc --d=1 --ef=2" => {"d":"1", "ef":"2} */
    override fun getNamed() = args.filter { it.startsWith("--") }.map { it.removePrefix("--").split(Regex("="), 2)  }.associate { (l, r) -> l to r }
}