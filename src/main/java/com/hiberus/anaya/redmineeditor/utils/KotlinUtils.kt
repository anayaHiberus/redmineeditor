package com.hiberus.anaya.redmineeditor.utils

import java.io.FileNotFoundException

///**
// * If this is true, run function (inline if/then)
// */
//fun Boolean.ifOK(function: () -> Unit) = this.also { if (it) function() }

/**
 * If this is not true, run function (inline if/else)
 */
fun Boolean.ifNotOK(function: () -> Unit) = this.also { if (!it) function() }

/**
 * returns the module of a class, as path
 */
fun Class<*>.getModuleResource(filename: String) =
    "/${module.name.replace(".", "/")}/$filename".let {
        getResource(it) ?: throw FileNotFoundException(it)
    }

/**
 * Kotlin has an error, and you can't use Throwable#printStackTrace because "Symbol is declared in module 'java.base' which does not export package 'kotlin'"
 */
fun Throwable.printStackTraceFix() = System.err.println(stackTraceToString())