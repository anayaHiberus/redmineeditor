package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.components.VERSION
import com.hiberus.anaya.redmineeditor.utils.addButton
import com.hiberus.anaya.redmineeditor.utils.clearButtons
import com.hiberus.anaya.redmineeditor.utils.openInBrowser
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane

/* ------------------------- about ------------------------- */

const val NAME = "Redmine editor"
const val AUTHOR = "Abel Naya"
val CONTRIBUTORS = listOf<String>()

val TITLE = "$NAME, by $AUTHOR, V$VERSION"

/**
 * Show the 'about' dialog
 */
fun ShowAbout() {
    Alert(Alert.AlertType.INFORMATION).apply {
        headerText = "About"
        contentText = """
            |$NAME.
            |App made by $AUTHOR.
            |Current version: $VERSION.
            |
            |Thanks to all testers and users!
            |Want to contribute? All help is welcomed!
            |${CONTRIBUTORS.takeIf { it.isNotEmpty() }?.joinToString("\n", prefix = "Contributors:\n") { "> $it" } ?: ""}
            |""".trimMargin()
        stylize()
        clearButtons()
        addButton(ButtonType("Source code")) {
            openInBrowser("https://gitlabdes.hiberus.com/anaya/redmineeditor")
        }
        addButton(ButtonType.CLOSE)
    }.showAndWait()
}

/* ------------------------- readme ------------------------- */

/**
 * Show the readme dialog
 */
fun ShowReadme() {
    Alert(Alert.AlertType.INFORMATION).apply {
        headerText = "Readme"
        dialogPane.content = ScrollPane(Label(Resources.getFile("Readme.txt").readText()))
            .apply { maxWidth = 100.0; maxHeight = 50.0 }
        stylize()
    }.showAndWait()
}