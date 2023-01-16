package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.ResourceFile
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.*
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

        // check new versions in background if allowed
        if (AppSettings.CHECK_UPDATES.value.toBoolean() || AppSettings.CHECK_SCHEDULE_UPDATES.value.toBoolean()) {
            daemonThread {

                // check updated app
                if (AppSettings.CHECK_UPDATES.value.toBoolean()) runCatching {
                    getNewVersion()?.let {
                        label.text = "New app version found: $it. Press here to download it."
                        action = { openDownloadUpdatePage() } // open remote page
                        banner.isVisible = true
                        return@daemonThread
                    }
                }.onFailure { debugln("Can't get remote version, probably not permission, ignoring: $it") }

                // check schedule update
                if (AppSettings.CHECK_SCHEDULE_UPDATES.value.toBoolean()) runCatching {
                    getNewScheduleFile()?.let { content ->
                        label.text = "Schedule file update found. Press here to update it."
                        action = {
                            replaceScheduleContent(content) {
                                close()
                                AppController.reload()
                            }
                        }
                        banner.isVisible = true
                        return@daemonThread
                    }
                }.onFailure { debugln("Can't get remote schedule file, either doesn't exists or not permission, ignoring: $it") }

            }
        }
    }

    @FXML
    fun close() {
        // close banner
        banner.isVisible = false
    }


    private var action: (() -> Unit)? = null // the new schedule

    @FXML
    fun onClick() = action?.let { it() } // run action

}

/* ------------------------- remote ------------------------- */

/**
 * Returns the new remote version, or null if no new version was detected
 */
fun getNewVersion() = URL("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/raw/javafx/src/main/resources/com/hiberus/anaya/redmineeditor/version").readText()
    .takeIf { it != VERSION }

/**
 * Returns the new content of the calendars file, if different
 */
fun getNewScheduleFile(calendar: String? = null) = URL("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/raw/javafx/${getCalendarFile(calendar)}").readText()
    .takeIf { areNewerRules(it.lineSequence(), calendar) }

/**
 * Opens the download page
 */
fun openDownloadUpdatePage() = openInBrowser("https://gitlabdes.hiberus.com/anaya/redmineeditor/-/tree/javafx/build")
