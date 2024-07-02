package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.READ_ONLY
import com.hiberus.anaya.redmineapi.Redmine
import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.components.VERSION
import com.hiberus.anaya.redmineeditor.components.getNewCalendarFile
import com.hiberus.anaya.redmineeditor.components.getNewVersion
import com.hiberus.anaya.redmineeditor.components.openDownloadUpdatePage
import com.hiberus.anaya.redmineeditor.model.*
import com.hiberus.anaya.redmineeditor.utils.*
import com.hiberus.anaya.tools.IgnoreSSLErrors
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.Property
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import java.util.*

/** Displays the settings dialog, and reloads if something changed */
fun ShowSettingsDialog() {
    val changes = ShowSettingsDialogInternal()
    if (AppSettings.DARK_THEME in changes) stylizeDisplayed()
    if (AppSettings.MARK_USED in changes) AppController.fireChanges(setOf(ChangeEvent.EntryContent))
    if (AppSettings.READ_ONLY in changes) READ_ONLY = AppSettings.READ_ONLY.value.toBoolean()
    if (AppSettings.IGNORE_SSL_ERRORS in changes) IgnoreSSLErrors() // this will not 'turn off' when disabled, but that's currently not possible
    if (ReloadSettings intersects changes) AppController.reload()
}

/** Displays the settings configuration dialog, returns the changes */
private fun ShowSettingsDialogInternal(): Set<AppSettings> {
    Stage().apply {
        title = "Settings"
        scene = Scene(FXMLLoader(ResourceLayout("settings")).load())
        scene.stylize()
        centerInMouseScreen()
        initModality(Modality.APPLICATION_MODAL)

        // show
        showAndWait()

        // return data
        return (scene.window.userData as? Set<*>)?.filterIsInstance<AppSettings>()?.toSet() ?: emptySet()
    }
}

/** The settings controller */
class SettingsController {

    /* ------------------------- nodes ------------------------- */

    lateinit var parent: Node // parent node
    private val window get() = parent.scene.window // window

    lateinit var domain: TextField // domain setting
    lateinit var predefined: MenuButton
    lateinit var key: PasswordField // key setting
    lateinit var testLoading: ProgressIndicator // loading indicator for testing api
    lateinit var testInfo: Label // info about api test
    lateinit var allowGetOnly: CheckBox // allow get only setting
    lateinit var ignoreSSLErrors: CheckBox // ignore ssl errors
    lateinit var autoLoadTotal: CheckBox // autoload total hours setting
    lateinit var autoLoadAssigned: CheckBox // autoload assigned issues setting
    lateinit var fixedIssues: TextField // issues to always load
    lateinit var calendar: MenuButton // Office from which to extract public holidays
    lateinit var checkCalendarUpdates: CheckBox // check calendar update loading indicator
    lateinit var calendarUpdateInfo: Label // check calendar update info
    lateinit var calendarUpdateLoading: ProgressIndicator // check calendar update checkbox
    lateinit var ignoreOldAssigned: Spinner<Any> // ignore old assigned number selector
    lateinit var prevDays: Spinner<Int> // number of previous days number selector
    lateinit var dark: CheckBox // dark theme setting
    lateinit var mark_used: MenuButton // mark used spinner
    lateinit var appUpdateLoading: ProgressIndicator // check update loading indicator
    lateinit var appUpdateInfo: Label // check update info
    lateinit var checkAppUpdate: CheckBox // check updates checkbox
    lateinit var save: Button // save button

    /* ------------------------- config ------------------------- */

    /** Keeps data about an AppSetting and how to manage it */
    private data class SettingMatch<T>(
        val setting: AppSettings,
        private val nodeProperty: () -> Property<T>,
        val valueConverter: (String) -> T
    ) {
        val property get() = nodeProperty() // get property
    }

    /** All the AppSetting configurations */
    private val matches = listOf(
        SettingMatch(AppSettings.URL, { domain.textProperty() }) { it },
        SettingMatch(AppSettings.KEY, { key.textProperty() }) { it },
        SettingMatch(AppSettings.READ_ONLY, { allowGetOnly.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.IGNORE_SSL_ERRORS, { ignoreSSLErrors.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.AUTO_LOAD_TOTAL_HOURS, { autoLoadTotal.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.AUTO_LOAD_ASSIGNED, { autoLoadAssigned.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.FIXED_ISSUES, { fixedIssues.textProperty() }) { it },
        SettingMatch(AppSettings.IGNORE_OLD_ASSIGNED, { ignoreOldAssigned.valueFactory.valueProperty() }) { it.toInt() },
        SettingMatch(AppSettings.PREV_DAYS, { prevDays.valueFactory.valueProperty() }) { it.toInt() },
        SettingMatch(AppSettings.DARK_THEME, { dark.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.MARK_USED, { mark_used.textProperty() }) { it.lowercase() },
        SettingMatch(AppSettings.CHECK_UPDATES, { checkAppUpdate.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.CHECK_SCHEDULE_UPDATES, { checkCalendarUpdates.selectedProperty() }) { it.toBoolean() },
        SettingMatch(AppSettings.SCHEDULE_FILE, { calendar.textProperty() }) { it },
    )

    /* ------------------------- functions ------------------------- */

    @FXML
    fun initialize() {
        // register callback to closing event
        Platform.runLater {
            window.setOnCloseRequest { closeWindowEvent() }
        }

        // prepare loading spinners
        listOf(testLoading, appUpdateLoading, calendarUpdateLoading).forEach {
            it.syncInvisible()
            it.isVisible = false
        }

        // syn dark setting with dark theme
        dark.selectedProperty().addListener { _, _, isDark ->
            parent.scene?.stylize(isDark)
        }

        // mark used enum
        with(mark_used) {
            items += MarkUsed.entries.map {
                MenuItem(it.name.lowercase()).apply {
                    onAction = EventHandler {
                        mark_used.text = text
                    }
                }
            }
        }

        // predefined options
        with(predefined) {
            // get from file, if exists
            val options = Properties().apply {
                getRelativeFile("conf/predefined.properties")?.inputStream()?.use { load(it) }
            }
            if (options.isEmpty) {
                // no entries, hide button
                syncInvisible()
                isVisible = false
            } else {
                // entries, add as menus
                items += options.map { (name, value) ->
                    MenuItem(name as String?).apply {
                        onAction = EventHandler {
                            domain.text = value as String?
                        }
                    }
                }
            }
        }

        // populate calendar selector
        with(calendar) {
            // Get every file on folder
            val calendarFiles = getAllCalendars()
            if (calendarFiles.isEmpty()) {
                // no entries
                items += MenuItem("<no files found>")
            } else {
                // entries, add as menus
                items += calendarFiles.map {
                    MenuItem(it).apply {
                        onAction = EventHandler {
                            calendar.text = text
                        }
                    }
                }
            }
        }

        // initialize properties
        matches.letEach { property.value = valueConverter(setting.value) }

        // configure loading spinners
        calendar.textProperty().addListener { _, _, _ ->
            calendarUpdateLoading.isVisible = false
            calendarUpdateInfo.text = ""
            calendarUpdateInfo.backgroundColor = null
        }
        listOf(domain, key).forEach {
            it.textProperty().addListener { _, _, _ ->
                testLoading.isVisible = false
                testInfo.text = ""
                testInfo.backgroundColor = null
            }
        }


        // intelligent save button
        with({
            // check and update lambda
            val changes = changes()
            save.text = when {
                ReloadSettings intersects changes -> "_Save & reload"
                changes.isNotEmpty() -> "_Save & apply"
                else -> "_Save"
            }
            save.enabled = changes.isNotEmpty()
        }) {
            // apply to all properties
            matches.map { it.property }.forEach {
                it.addListener { _, _, _ -> this() }
            }
            this()
        }
    }

    @FXML
    fun instructions() {
        // show instructions to fill the key
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "API key instructions"
            headerText = "Fill this value with your Redmine API key"
            dialogPane.contentText = "You can find it in Redmine -> my account -> api key (or press the button below to show it directly)"

            // set buttons
            clearButtons()
            val openButton = addButton(ButtonType("Open")) {
                openInBrowser(domain.text.ensureSuffix("/") + "my/api_key")
            }
            addButton(ButtonType.CLOSE)

            // disable open if no domain
            if (domain.text.isBlank()) {
                openButton.enabled = false
                dialogPane.contentText += "\n\n(note: fill the domain and open these instructions again to enable the open button)"
            }

            stylize(dark.isSelected)
        }.showAndWait() // display
    }

    @FXML
    fun testAPI() {
        // test domain and key
        testLoading.isVisible = true
        testInfo.text = ""
        testInfo.backgroundColor = null

        // enable if required
        if (ignoreSSLErrors.isSelected) IgnoreSSLErrors()

        // run in background
        daemonThread {
            val result = runCatching {
                "OK: Valid settings found for user " + Redmine(domain.text, key.text, 0).getUserName(appendLogin = true)
            }.getOrNull()

            // then notify in foreground
            Platform.runLater {
                testInfo.text = result ?: "ERROR: Invalid settings, can't connect to service"
                testInfo.backgroundColor = if (result != null) Color.LIGHTGREEN else Color.INDIANRED
                testLoading.isVisible = false
            }
        }
    }

    @FXML
    fun checkAppUpdate() {
        // check update now
        appUpdateLoading.isVisible = true
        appUpdateInfo.text = ""
        appUpdateInfo.backgroundColor = null

        // run in background
        daemonThread {
            val (result, color) = runCatching {
                // if ok,
                getNewVersion()?.let { "Update found: $it. Click here to download." to Color.LIGHTGREEN }
                    ?: ("No update found, using latest version: $VERSION. Click here to download anyway." to null)
            }.getOrElse { "ERROR: Can't check update, unable to get remote version" to Color.INDIANRED }

            // then notify in foreground
            Platform.runLater {
                appUpdateInfo.text = result
                appUpdateInfo.backgroundColor = color
                appUpdateLoading.isVisible = false
            }
        }
    }

    @FXML
    fun checkCalendarUpdate() {
        // check update now
        calendarUpdateLoading.isVisible = true
        calendarUpdateInfo.text = ""
        calendarUpdateInfo.backgroundColor = null

        // run in background
        daemonThread {
            val (result, color) = runCatching {
                // if ok,
                getNewCalendarFile(calendar.text)?.let { "Update found. Click here to replace local file." to Color.LIGHTGREEN }
                    ?: ("No update found, using latest version." to null)
            }.getOrElse { "ERROR: Can't check update, unable to get remote file" to Color.INDIANRED }

            // then notify in foreground
            Platform.runLater {
                calendarUpdateInfo.text = result
                calendarUpdateInfo.backgroundColor = color
                calendarUpdateLoading.isVisible = false
            }
        }
    }

    @FXML
    fun downloadAppUpdate() = openDownloadUpdatePage()

    @FXML
    fun downloadCalendarUpdate() = calendarUpdateInfo.text.contains("Click here").ifOK {
        getNewCalendarFile(calendar.text)?.let {
            replaceCalendarContent(it, calendar.text) {
                calendarUpdateInfo.text = "Replaced"
                calendarUpdateInfo.backgroundColor = Color.GREEN
                calendarUpdateLoading.isVisible = false
            }
        }
    }

    @FXML
    fun loadDefault() {
        // load default settings, but ask first
        Alert(Alert.AlertType.CONFIRMATION, "Do you want to discard all data and load all default settings?")
            .apply {
                stylize(dark.isSelected)
                addButton(ButtonType.OK) {
                    matches.letEach { property.value = valueConverter(setting.default) }
                }
            }.showAndWait()
    }

    @FXML
    fun save() {
        // save settings
        window.userData = changes(apply = true)
        // and exit
        window.hide()
    }

    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

    /* ------------------------- internal ------------------------- */

    /** List of changes */
    private fun changes(apply: Boolean = false) =
        // for all changes
        matches.map { it.setting to it.property.value.toString() }
            // keep those modified
            .filter { (setting, value) ->
                if (apply) setting.modify(value) // save if required
                else setting.value != value
                // return modified settings
            }.map { it.first }.toSet()

    /** On window closes, asks to lose changes if any */
    private fun closeWindowEvent() {
        if (changes().isNotEmpty() && !confirmLoseChanges("exit")) window.hide()
    }

}

/** Command to manage settings. */
class SettingsCommand : Command {
    override val name = "View/Modify settings from the command line"
    override val argument = "-settings"
    override val parameters = "[--name=value]* [-list]"
    override val help = listOf(
        "-list : will display all settings with their values",
        "--name=value : will set <value> for the property <name>",
        "WARNING! values will not be checked for correctness, if the app doesn't load afterwards, restore the original value",
    )

    override fun run(parameters: Application.Parameters) {
        // list values
        if (parameters.unnamed.contains("list")) {
            println("All Settings:")
            AppSettings.entries.map { entry ->
                println("${entry.name}=${entry.value} [default: '${entry.default}']")
            }
        }

        // set values
        parameters.named.forEach { (name, value) ->
            runCatching { AppSettings.valueOf(name.uppercase()) }
                .onSuccess { appSetting ->
                    val previous = appSetting.value
                    appSetting.value = value
                    println("Replaced $name from '$previous' to '$value' (default value is '${appSetting.default}')")
                }.onFailure {
                    println("There is no setting with name '$name'")
                }
        }
    }

}