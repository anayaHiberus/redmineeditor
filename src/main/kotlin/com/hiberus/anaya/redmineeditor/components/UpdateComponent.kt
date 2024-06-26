package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.ResourceFile
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import org.json.JSONObject
import java.net.URL
import java.util.*

/** The current app version (as the version file says) */
val VERSION = runCatching { ResourceFile("version").readText().trim() }.getOrDefault("?.?.?")

/**
 * A banner at the top that checks for updates
 * TODO: convert to a generic banner component and extract updating logic to another file
 */
internal class UpdateComponent {

    /* ------------------------- views ------------------------- */

    lateinit var banner: HBox // the banner parent
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

/** Returns the new remote version, or null if no new version was detected */
fun getNewVersion() = JSONObject(URL("https://api.github.com/repos/anayaHiberus/redmineeditor/releases/latest").readText()).getString("tag_name").drop(1)
    // compare versions based on the numbers ("2.1" < "2.2" but "2.1.1" > "2.1")
    .takeIf { remoteVersion ->
        listOf(remoteVersion, VERSION)
            .map { version ->
                version.split(".")
                    .map { it.toIntOrNull() ?: 0 }
                    .toIntArray()
            }.let { (r, c) -> Arrays.compare(r, c) > 0 }
    }

/** Returns the new content of the calendars file, if different */
fun getNewScheduleFile(calendar: String? = null) = URL("https://raw.githubusercontent.com/anayaHiberus/redmineeditor/main/${getCalendarFile(calendar)}").readText()
    .takeIf { areNewerRules(it.lineSequence(), calendar) }

/** Opens the download page */
fun openDownloadUpdatePage() = openInBrowser("https://github.com/anayaHiberus/redmineeditor/releases/latest")
