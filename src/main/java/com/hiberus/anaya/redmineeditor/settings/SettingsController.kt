package com.hiberus.anaya.redmineeditor.settings

import com.hiberus.anaya.redmineapi.Redmine
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import kotlin.concurrent.thread

/**
 * The settings controller
 */
class SettingsController {

    companion object {
        /**
         * Displays the settings configuration dialog
         */
        fun show(): Boolean {
            Stage().apply {
                title = "Settings"
                scene = Scene(FXMLLoader(object {}.javaClass.getModuleResource("settings.fxml")).load())
                    .apply { stylize() }
                initModality(Modality.APPLICATION_MODAL)

                showAndWait()

                return scene.window.userData != null
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
    lateinit var key: PasswordField // key setting

    @FXML
    lateinit var testLoading: ProgressIndicator // loading indicator for testing api

    @FXML
    lateinit var testInfo: Label // info about api test

    @FXML
    lateinit var allowGetOnly: CheckBox // allow get only setting

    @FXML
    lateinit var autoLoadTotal: CheckBox // auto load total hours setting

    @FXML
    lateinit var prevDays: Spinner<Int> // number of previous days setting

    @FXML
    lateinit var dark: CheckBox // dark theme setting

    /* ------------------------- functions ------------------------- */

    @FXML
    fun initialize() {
        // register callback to closing event
        Platform.runLater {
//            window.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent)
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
            testInfo.apply { setBackgroundColor(userData as Color?, isDark) }
        }

        // initialize properties
        domain.text = AppSettings.URL.value
        key.text = AppSettings.KEY.value
        allowGetOnly.isSelected = AppSettings.READ_ONLY.value.toBoolean()
        autoLoadTotal.isSelected = AppSettings.AUTO_LOAD_TOTAL_HOURS.value.toBoolean()
        prevDays.valueFactory.value = AppSettings.PREV_DAYS.value.toInt()
        dark.isSelected = AppSettings.DARK_THEME.value.toBoolean()
    }

    @FXML
    fun hiberus() {
        // restore hiberus default setting
        domain.text = "https://redmine.hiberus.com/redmine/"
    }

    @FXML
    fun instructions() {
        // show instructions to fill the key
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "API key instructions"
            headerText = "Fill this value with your Redmine API key"
            contentText = """
                |1) Go to <a href="https://redmine.hiberus.com/redmine/my/account">myPage</a>
                |2) Press on 'Show' at the right
                |3) Copy the key and paste it here
                |""".trimMargin()
            stylize(dark.isSelected)
        }.showAndWait() // display
    }

    @FXML
    fun testAPI() {
        // test domain and key
        testLoading.isVisible = true
        testInfo.text = ""
        colorizeTestAPIResult(null)

        // run in background
        thread(isDaemon = true) {
            val result = runCatching {
                "OK: Valid settings found for user " + Redmine(domain.text, key.text, true, 0).getUserName()
            }.getOrNull()

            // then notify in foreground
            Platform.runLater {
                testInfo.text = result ?: "ERROR: Invalid settings, can't connect to service"
                colorizeTestAPIResult(if (result != null) Color.GREEN else Color.RED)
                testLoading.isVisible = false
            }
        }
    }

    private fun colorizeTestAPIResult(color: Color?) {
        testInfo.setBackgroundColor(color, dark.isSelected)
        testInfo.userData = color
    }

    @FXML
    fun loadDefault() {
        // load default settings, but ask first
        Alert(Alert.AlertType.CONFIRMATION, "Do you want to discard all data and load all default settings?")
            .apply { stylize(dark.isSelected) }
            .showAndWait()
            .run { resultButton == ButtonType.OK }.ifOK {
                domain.text = AppSettings.URL.default
                key.text = AppSettings.KEY.default
                allowGetOnly.isSelected = AppSettings.READ_ONLY.default.toBoolean()
                autoLoadTotal.isSelected = AppSettings.AUTO_LOAD_TOTAL_HOURS.default.toBoolean()
                prevDays.valueFactory.value = AppSettings.PREV_DAYS.default.toInt()
                dark.isSelected = AppSettings.DARK_THEME.default.toBoolean()
            }
    }

    @FXML
    fun save() {
        // save settings
        AppSettings.URL.value = domain.text
        AppSettings.KEY.value = key.text
        AppSettings.READ_ONLY.value = allowGetOnly.isSelected.toString()
        AppSettings.AUTO_LOAD_TOTAL_HOURS.value = autoLoadTotal.isSelected.toString()
        AppSettings.PREV_DAYS.value = prevDays.valueFactory.value.toString()
        AppSettings.DARK_THEME.value = dark.isSelected.toString()
        AppSettings.save()
        // and exit

        window.userData = true
        window.hide()
    }

    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

    /* ------------------------- internal ------------------------- */

    private fun hasChanges() =
        AppSettings.URL.value != domain.text
                || AppSettings.KEY.value != key.text
                || AppSettings.READ_ONLY.value != allowGetOnly.isSelected.toString()
                || AppSettings.AUTO_LOAD_TOTAL_HOURS.value != autoLoadTotal.isSelected.toString()
                || AppSettings.PREV_DAYS.value != prevDays.valueFactory.value.toString()
                || AppSettings.DARK_THEME.value != dark.isSelected.toString()

    /**
     * On window closes, asks to lose changes if any
     */
    private fun closeWindowEvent() {
        if (hasChanges() && !confirmLoseChanges("exit")) window.hide()
    }

}