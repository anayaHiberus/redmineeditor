package com.hiberus.anaya.redmineeditor

import com.hiberus.anaya.redmineapi.READ_ONLY
import com.hiberus.anaya.redmineeditor.dialogs.FixMonthTool
import com.hiberus.anaya.redmineeditor.dialogs.TITLE
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import java.util.concurrent.CountDownLatch

/**
 * Main app
 */
class Main : Application() {

    /**
     * Starts the app process
     */
    override fun init() {

        // read only
        READ_ONLY = "-read_only" in parameters.unnamed

        if ("-h" in parameters.raw) {
            println("This is a test, it works!")
            Platform.exit()
        }

        if ("-fix" in parameters.raw) {

            val issueId = parameters.named["issue"]?.toIntOrNull() ?: run {
                System.err.println(if ("issue" in parameters.named) parameters.named["issue"] + " is not an integer" else "Missing issue parameter")
                Platform.exit()
                return
            }
            val comment = parameters.named["comment"] ?: "".also { println("No comment provided, an empty string will be used") }
            val week = ("week" in parameters.unnamed).also { println("Fixing " + if (it) "week" else "month") }
            val future = ("future" in parameters.unnamed).also { println("Fixing " + if (it) "future days too" else "past days only") }


            val latch = CountDownLatch(1)
            var step = 1
            AppController.onChanges(setOf(ChangeEvent.Loading)) { readOnlyModel ->
                println("step: $step (${readOnlyModel.isLoading})")
                if (readOnlyModel.isLoading) return@onChanges
                when (step) {
                    1 -> AppController.reload(askIfChanges = false, resetDay = true)
                    2 -> AppController.runBackground { it.loadIssues(listOf(issueId)) }
                    3 -> readOnlyModel.loadedIssues
                        ?.find { it.id == issueId }
                        ?.let { FixMonthTool(it, comment, week, future) { latch.countDown() } }
                        ?: run {
                            System.err.println("No issue with id $issueId was found")
                            latch.countDown()
                        }
                }
                step++
            }

            AppController.runBackground { /* start process */ }
            latch.await()
            Platform.exit()
        }
    }


    /**
     * Starts the app window (stage)
     */
    override fun start(stage: Stage) = stage.run {
        // load and show
        title = TITLE
        scene = Scene(FXMLLoader(Resources.getLayout("parent")).load())
        scene.stylize()
        show()
    }

}

