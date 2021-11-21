package com.hiberus.anaya.redmineeditor

import java.io.FileNotFoundException

/**
 * Represents the app resources and how to find them
 */
class Resources {
    companion object {

        /**
         * Returns an arbitrary file from its [filename]
         */
        // Implementation notes: make sure this class/file is in the main directory, or at least make sure the subdirectories match
        fun getFile(filename: String) = this::class.java.getResource(filename) ?: throw FileNotFoundException(filename)

        /**
         * Gets a layout file (same as [getFile] with "layouts/[name].fxml"
         */
        fun getLayout(name: String) = getFile("layouts/$name.fxml")


        /**
         * Gets an image file (same as [getFile] with "images/[name].png"
         */
        fun getImage(name: String) = getFile("images/$name.png")

    }
}