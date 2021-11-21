package com.hiberus.anaya.redmineeditor.utils

/**
 * If this is true, run function (inline if/then)
 */
inline fun Boolean.ifOK(function: () -> Unit) = this.also { if (it) function() }

/**
 * If this is not true, run function (inline if/else)
 */
inline fun Boolean.ifNotOK(function: () -> Unit) = this.also { if (!it) function() }

/**
 * Kotlin has an error, and you can't use Throwable#printStackTrace because "Symbol is declared in module 'java.base' which does not export package 'kotlin'"
 */
fun Throwable.printStackTraceFix() = System.err.println(stackTraceToString())