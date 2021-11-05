package com.hiberus.anaya.redmineeditor

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * Main app
 */
fun main(args: Array<String>) = Application.launch(Main::class.java, *args)

class Main : Application() {

    override fun start(stage: Stage) = stage.run {
        // load and show
        title = "Redmine editor, by Abel Naya (V 0.2 alpha)"
        scene = Scene(FXMLLoader(this@Main.javaClass.getResource("parent.fxml")).load())
        show()
    }

}

