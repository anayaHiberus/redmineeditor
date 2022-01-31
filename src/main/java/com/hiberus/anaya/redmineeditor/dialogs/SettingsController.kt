package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Redmine
import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
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
import kotlin.concurrent.thread

/**
 * The settings controller
 */
class SettingsController {

    companion object {
        /**
         * Displays the settings configuration dialog
         */
        fun show(): Set<AppSettings> {
            Stage().apply {
                title = "Settings"
                scene = Scene(FXMLLoader(Resources.getLayout("settings")).load())
                    .apply { stylize() }
                initModality(Modality.APPLICATION_MODAL)

                showAndWait()

                return (scene.window.userData as? Set<*>)?.filterIsInstance<AppSettings>()?.toSet() ?: emptySet()
            }
        }
    }

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
    lateinit var prevDays: Spinner<Int> // number of previous days setting

    @FXML
    lateinit var dark: CheckBox // dark theme setting

    @FXML
    lateinit var save: Button // save button

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
        domain.text = AppSettings.URL.value
        key.text = AppSettings.KEY.value
        allowGetOnly.isSelected = AppSettings.READ_ONLY.value.toBoolean()
        autoLoadTotal.isSelected = AppSettings.AUTO_LOAD_TOTAL_HOURS.value.toBoolean()
        prevDays.valueFactory.value = AppSettings.PREV_DAYS.value.toInt()
        dark.isSelected = AppSettings.DARK_THEME.value.toBoolean()

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
            listOf(
                domain.textProperty(),
                key.textProperty(),
                allowGetOnly.selectedProperty(),
                autoLoadTotal.selectedProperty(),
                prevDays.valueProperty(),
                dark.selectedProperty()
            ).forEach {
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
        thread(isDaemon = true) {
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
                    domain.text = AppSettings.URL.default
                    key.text = AppSettings.KEY.default
                    allowGetOnly.isSelected = AppSettings.READ_ONLY.default.toBoolean()
                    autoLoadTotal.isSelected = AppSettings.AUTO_LOAD_TOTAL_HOURS.default.toBoolean()
                    prevDays.valueFactory.value = AppSettings.PREV_DAYS.default.toInt()
                    dark.isSelected = AppSettings.DARK_THEME.default.toBoolean()
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
        listOf(
            AppSettings.URL to domain.text,
            AppSettings.KEY to key.text,
            AppSettings.READ_ONLY to allowGetOnly.isSelected,
            AppSettings.AUTO_LOAD_TOTAL_HOURS to autoLoadTotal.isSelected,
            AppSettings.PREV_DAYS to prevDays.valueFactory.value,
            AppSettings.DARK_THEME to dark.isSelected,
        ).filter { (setting, value) ->
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