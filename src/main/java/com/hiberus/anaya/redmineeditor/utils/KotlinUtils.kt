package com.hiberus.anaya.redmineeditor.utils

/**
 * If this is true, run function (inline if/then)
 */
fun Boolean.ifOK(function: () -> Unit) = this.also { if (it) function() }

/**
 * If this is not true, run function (inline if/else)
 */
fun Boolean.ifNotOK(function: () -> Unit) = this.also { if (!it) function() }
