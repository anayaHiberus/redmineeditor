package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.READ_ONLY
import com.hiberus.anaya.redmineapi.Redmine
import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.model.ReloadSettings
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.beans.property.ReadOnlyProperty
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

/**
 * Displays the settings dialog, and reloads if something changed
 */
fun ShowSettingsDialog() {
    val changes = ShowSettingsDialogInternal()
    if (AppSettings.DARK_THEME in changes) stylizeDisplayed()
    if (AppSettings.READ_ONLY in changes) READ_ONLY = AppSettings.READ_ONLY.value.toBoolean()
    if (ReloadSettings intersects changes) AppController.reload()
}

/**
 * Displays the settings configuration dialog, returns the changes
 */
private fun ShowSettingsDialogInternal(): Set<AppSettings> {
    Stage().apply {
        title = "Settings"
        scene = Scene(FXMLLoader(Resources.getLayout("settings")).load())
            .apply { stylize() }
        initModality(Modality.APPLICATION_MODAL)

        // show
        showAndWait()

        // return data
        return (scene.window.userData as? Set<*>)?.filterIsInstance<AppSettings>()?.toSet() ?: emptySet()
    }
}

/**
 * The settings controller
 */
class SettingsController {
    // TODO: somehow simplify the setters/getters of all settings

    /* ------------------------- nodes ------------------------- */

    @FXML
    lateinit var parent: Node // parent node
    private val window get() = parent.scene.window // window

    @FXML
    lateinit var domain: TextField // domain setting

    @FXML
    lateinit var predefined: MenuButton

    @FXML
    lateinit var key: PasswordField // key setting

    @FXML
    lateinit var testLoading: ProgressIndicator // loading indicator for testing api

    @FXML
    lateinit var testInfo: Label // info about api test

    @FXML
    lateinit var allowGetOnly: CheckBox // allow get only setting

    @FXML
    lateinit var autoLoadTotal: CheckBox // autoload total hours setting

    @FXML
    lateinit var autoLoadAssigned: CheckBox // autoload assigned issues setting

    @FXML
    lateinit var prevDays: Spinner<Int> // number of previous days setting

    @FXML
    lateinit var dark: CheckBox // dark theme setting

    @FXML
    lateinit var save: Button // save button

    /* ------------------------- config ------------------------- */

    /**
     * Keeps data about an AppSetting and how to manage it
     */
    private data class SettingMatch<T>(val setting: AppSettings, val nodeGetter: () -> T, val nodeSetter: (String) -> Unit, val nodeProperty: () -> ReadOnlyProperty<*>) {
        fun init() = nodeSetter(setting.value)
        fun default() = nodeSetter(setting.default)
        val data get() = setting to nodeGetter()
        val property get() = nodeProperty()
    }

    /**
     * All the AppSetting configurations
     */
    private val matches = listOf(
        SettingMatch(AppSettings.URL, { domain.text }, { domain.text = it }) { domain.textProperty() },
        SettingMatch(AppSettings.KEY, { key.text }, { key.text = it }) { key.textProperty() },
        SettingMatch(AppSettings.READ_ONLY, { allowGetOnly.isSelected }, { allowGetOnly.isSelected = it.toBoolean() }) { allowGetOnly.textProperty() },
        SettingMatch(AppSettings.AUTO_LOAD_TOTAL_HOURS, { autoLoadTotal.isSelected }, { autoLoadTotal.isSelected = it.toBoolean() }) { autoLoadTotal.textProperty() },
        SettingMatch(AppSettings.AUTO_LOAD_ASSIGNED, { autoLoadAssigned.isSelected }, { autoLoadAssigned.isSelected = it.toBoolean() }) { autoLoadAssigned.textProperty() },
        SettingMatch(AppSettings.PREV_DAYS, { prevDays.valueFactory.value }, { prevDays.valueFactory.value = it.toInt() }) { prevDays.valueProperty() },
        SettingMatch(AppSettings.DARK_THEME, { dark.isSelected }, { dark.isSelected = it.toBoolean() }) { dark.selectedProperty() },
    )

    /* ------------------------- functions ------------------------- */

    @FXML
    fun initialize() {
        // register callback to closing event
        Platform.runLater {
            window.setOnCloseRequest { closeWindowEvent() }
        }

        // prepare testLoading
        testLoading.run {
            syncInvisible()
            isVisible = false
        }

        // syn dark setting with dark theme
        dark.selectedProperty().addListener { _, _, isDark ->
            parent.scene?.stylize(isDark)
        }

        // predefined options
        with(predefined) {
            // get from file, if exists
            val options = Properties().apply { findFile("conf/predefined.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
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

        // initialize properties
        matches.forEach { it.init() }

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
            dialogPane.contentText = "You can find it in Redmine -> my page -> api key."

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
    fun loadDefault() {
        // load default settings, but ask first
        Alert(Alert.AlertType.CONFIRMATION, "Do you want to discard all data and load all default settings?")
            .apply {
                stylize(dark.isSelected)
                addButton(ButtonType.OK) {
                    matches.forEach { it.default() }
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

    /**
     * List of changes
     */
    private fun changes(apply: Boolean = false) =
        matches.map { it.data }
            .filter { (setting, value) ->
                if (apply) setting.modify(value)
                else setting.value != value.toString()
            }.map { it.first }.toSet()

    /**
     * On window closes, asks to lose changes if any
     */
    private fun closeWindowEvent() {
        if (changes().isNotEmpty() && !confirmLoseChanges("exit")) window.hide()
    }

}