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
import java.util.*
import kotlin.concurrent.thread

/**
 * Displays the settings configuration dialog
 */
fun ShowSettingsDialog() {
    Stage().apply {
        title = "Settings"
        scene = Scene(FXMLLoader(object {}.javaClass.getModuleResource("settings.fxml")).load())
            .also { it.stylize() }
        initModality(Modality.APPLICATION_MODAL)
    }.showAndWait()
}

/**
 * The settings controller
 */
class SettingsController {

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

    /* ------------------------- settings ------------------------- */

    private val data = DATA.clone() as Properties

    /* ------------------------- functions ------------------------- */

    @FXML
    fun initialize() {
        // register callback to closing event
        Platform.runLater {
            window.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST) {
                if (!data.isEmpty && !confirmLoseChanges("exit")) it.consume()
            }
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
        // TODO: fix this
        domain.text = data.getProperty(SETTING.URL.name, SETTING.URL.default)
        key.text = data.getProperty(SETTING.KEY.name, SETTING.KEY.default)
        allowGetOnly.isSelected = data.getProperty(SETTING.READ_ONLY.name, SETTING.READ_ONLY.default).toBoolean()
        autoLoadTotal.isSelected = data.getProperty(SETTING.AUTO_LOAD_TOTAL_HOURS.name, SETTING.AUTO_LOAD_TOTAL_HOURS.default).toBoolean()
        prevDays.valueFactory.value = data.getProperty(SETTING.PREV_DAYS.name, SETTING.PREV_DAYS.default).toInt()
        dark.isSelected = data.getProperty(SETTING.DARK_THEME.name, SETTING.DARK_THEME.default).toBoolean()

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
        // load default settings
        if (Alert(Alert.AlertType.CONFIRMATION, "Do you want to discard all data and load all default settings?").showAndWait().resultButton == ButtonType.YES) {
            hiberus()
            key.text = ""
        }
    }

    @FXML
    fun save() {
        // save settings
        findFile("conf/settings.properties").outputStream().use {
            data.store(it, "IT WORKS")
        }
        cancel()
    }

    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

}