package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.ResourceFile
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.daemonThread
import com.hiberus.anaya.redmineeditor.utils.debugln
import com.hiberus.anaya.redmineeditor.utils.openInBrowser
import com.hiberus.anaya.redmineeditor.utils.syncInvisible
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import java.net.URL

/**
 * The current app version (as the version file says)
 */
val VERSION = runCatching { ResourceFile("version").readText() }.getOrDefault("?.?.?")

/**
 * A banner at the top that checks for updates
 * TODO: convert to a generic banner component and extract updating logic to another file
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

        // check new version in background if allowed
        if (AppSettings.CHECK_UPDATES.value.toBoolean()) {
            daemonThread {
                runCatching {
                    getNewVersion()?.let {
                        label.text = "New version found: $it. Press here to download it."
                        banner.isVisible = true
                    }
                }.onFailure { debugln("Can't get remote version, probably not permission, ignoring: $it") }
            }
        }
    }

    @FXML
    fun close() {
        // close banner
        banner.isVisible = false
    }

    @FXML
    fun update() = openDownloadUpdatePage() // open remote page

}

/* ------------------------- remote ------------------------- */

/**
 * Returns the new remote version, or null if no new version was detected
 */
fun getNewVersion() = URL("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/raw/javafx/src/main/resources/com/hiberus/anaya/redmineeditor/version").readText()
    .takeIf { it != VERSION }

/**
 * Opens the download page
 */
fun openDownloadUpdatePage() = openInBrowser("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/tree/javafx/build")
