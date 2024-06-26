package com.hiberus.anaya.redmineeditor.utils

import kotlin.concurrent.thread

/** If this is true, run function (inline if/then) */
inline fun Boolean.ifOK(function: () -> Unit) = this.also { if (it) function() }

/** If this is not true, run function (inline if/else) */
inline fun Boolean.ifNotOK(function: () -> Unit) = this.also { if (!it) function() }

/** Makes sure the string ends with the suffix, if it doesn't append it */
fun String.ensureSuffix(suffix: String) = removeSuffix(suffix) + suffix

/** test if there is at least a common element in both sets */
infix fun <E> Set<E>.intersects(other: Set<E>) =
    (this intersect other).isNotEmpty()

/** For some reason threads are not daemon by default */
inline fun daemonThread(crossinline block: () -> Unit) = thread(isDaemon = true) { block() }

/** list.forEach { it.set(it.run(it.get())) } is ugly, list.letEach { set(run(get())) } is better */
inline fun <T> Iterable<T>.letEach(action: T.() -> Unit) = forEach { it.action() }

/** Extract and capitalize name of file */
fun String.extractFileName() = substringBeforeLast('.').replaceFirstChar { it.titlecase() }


/** Adds the element into the list, unless it already exists */
fun <E> MutableList<E>.put(item: E) = contains(item) or add(item)