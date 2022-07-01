package com.hiberus.anaya.redmineeditor

import java.io.FileNotFoundException

/**
 * An arbitrary resource file from its [filename]
 */
fun ResourceFile(filename: String) = Resources::class.java.getResource(filename) ?: throw FileNotFoundException(filename)

/**
 * A layout file (same as [ResourceFile] with "layouts/[name].fxml")
 */
fun ResourceLayout(name: String) = ResourceFile("layouts/$name.fxml")

/**
 * An image file (same as [ResourceFile] with "images/[name].png")
 */
fun ResourceImage(name: String) = ResourceFile("images/$name.png")

/* ------------------------- internal ------------------------- */

/**
 * This class must be defined in the same package (directory) as the root of the resources
 */
private class Resources