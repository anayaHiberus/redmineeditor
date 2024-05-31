package com.hiberus.anaya.redmineeditor.utils

/** true iff we are in debug mode (environment DEBUG=true) */
var IS_DEBUG = System.getenv("DEBUG").toBoolean()

/** Prints a message in debug mode only */
fun debugln(data: String) {
    if (IS_DEBUG) println("<DEBUG> $data")
}

/** prints a throwable message in debug mode only */
fun debugln(throwable: Throwable) {
    if (IS_DEBUG) System.err.println("<ERROR> " + throwable.stackTraceToString())
}

/** Prints an error in the standard error stream */
fun errorln(data: String) = System.err.println(data)

/** Prints the stacktrace in debug only. */
fun Throwable.debugPrintStackTrace() {
    if (IS_DEBUG) printStackTrace()
}