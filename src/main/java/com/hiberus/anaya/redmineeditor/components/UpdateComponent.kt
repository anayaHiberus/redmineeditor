package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.dialogs.VERSION
import com.hiberus.anaya.redmineeditor.utils.daemonThread
import com.hiberus.anaya.redmineeditor.utils.openInBrowser
import com.hiberus.anaya.redmineeditor.utils.syncInvisible
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import java.net.URL

/**
 * A banner at the top that checks for updates, probably good idea to convert to a generic banner component
 */
internal class UpdateComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var banner: HBox // the banner parent

    @FXML
    lateinit var label: Label // the label

    /* ------------------------- actions ------------------------- */

    @FXML
    fun initialize() {
        // start invisible
        banner.syncInvisible()
        close()

        // check new version in background
        daemonThread {
            runCatching {
                getNewVersion()?.let {
                    label.text = "New version found: $it. Press here to download it."
                    banner.isVisible = true
                }
            }.onFailure { println("Can't get remote version, probably not permission, ignoring: $it") }
        }
    }

    @FXML
    fun close() {
        // close banner
        banner.isVisible = false
    }

    @FXML
    fun update() {
        // open remote page
        openInBrowser("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/tree/javafx/build")
    }

}

/* ------------------------- remote ------------------------- */

/**
 * Returns the new remote version, or null if no new version was detected
 */
fun getNewVersion() = URL("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/raw/javafx/src/main/resources/com/hiberus/anaya/redmineeditor/version").readText()
    .takeIf { it != VERSION }

