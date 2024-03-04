package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.centerInMouseScreen
import com.hiberus.anaya.redmineeditor.utils.debugln
import com.hiberus.anaya.redmineeditor.utils.openInBrowser
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.web.WebView
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent


/**
 * Show an issue details as a dialog
 */
fun Issue.showDetails() {
    Stage().apply {
        title = toShortString()
        initModality(Modality.APPLICATION_MODAL)

        // initialize scene
        val loader = FXMLLoader(ResourceLayout("issue_description"))
        scene = Scene(loader.load())
        scene.stylize()
        centerInMouseScreen()

        // initialize now with the issue
        loader.getController<IssueDetailsController>().initialize(this@showDetails)

    }.showAndWait()
}

/**
 * The controller of the issue details dialog
 */
class IssueDetailsController {

    /* ------------------------- variables ------------------------- */

    lateinit var issue: Issue

    /* ------------------------- nodes ------------------------- */

    @FXML
    lateinit var description: Label

    @FXML
    lateinit var webview: WebView

    /* ------------------------- functions ------------------------- */

    // manually loaded, not by FXML, because we need to pass the parameter
    fun initialize(issue: Issue) {
        this.issue = issue

        // description
        description.text = issue.toString()

        // webview data
        val body = (
                // description
                listOf(issue.description.let { if (it.isNotEmpty()) "<h3>Description</h3><br>$it" else null })
                        // and journals
                        + issue.journals.mapIndexed { i, text -> if (text.isBlank()) null else "<h3>Journal #${i + 1}</h3><br>$text" }
                )
            // remove empty
            .filterNotNull()
            // join
            .joinToString("<hr>")
            // if no data
            .ifEmpty { "<p>no description nor journals to display</p>" }

        webview.engine.loadContent(
            // html data
            """
                <head>
                  <base href="${AppSettings.URL.value}" target="_blank">
                </head>
                <body>
                    <div>
                      $body
                    </div>
                    <script>
                    let mydiv = document.querySelector("body > div");
                    let allLoaded = false; 
                    Promise.all(Array.from(document.images).filter(img => !img.complete).map(img => new Promise(resolve => { img.onload = img.onerror = resolve; }))).then(() => {
                       allLoaded = true;
                    });
                    function getSize(){
                        if(!allLoaded) return "0 0";
                        else return mydiv.offsetHeight + ' ' + mydiv.offsetWidth;
                    }
                    </script>
                </body>
            """.trimIndent()
        )
        resize()
    }

    /**
     * Resizes the window based on the webview content
     * (which, for some reason, isn't done automatically)
     */
    private fun resize() {
        // wait if still loading
        if (webview.engine.loadWorker.isRunning) {
            Platform.runLater { resize() }
            return
        }

        runCatching {
            // get the parent div size
            val result: Any = webview.engine.executeScript("getSize()")
            if (result is String) {
                val (height, width) = result.split(" ").let { it[0].toDouble() to it[1].toDouble() }

                // invalid size, retry later
                if (height == 0.0 || width == 0.0) {
                    Platform.runLater { resize() }
                    return
                }

                // set and resize
                webview.prefHeight = height + 30
                webview.prefWidth = width + 30
                webview.scene.window.sizeToScene()
                webview.scene.window.centerOnScreen()
            }
        }.onFailure { debugln(it) }
    }


    @FXML
    fun openInRedmine() {
        // open issue url in the browser
        openInBrowser(issue.url)
    }

    @FXML
    fun close() = webview.scene.window.run { fireEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)) }

}