package com.hiberus.anaya.redmineeditor

import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

/**
 * Main app
 */
fun main(args: Array<String>) = Application.launch(Main::class.java, *args)

class Main : Application() {

    override fun start(stage: Stage) = stage.run {
        // load and show
        title = "Redmine editor, by Abel Naya (V 0.6 beta)"
        scene = Scene(FXMLLoader(Resources.getLayout("parent")).load())
            .apply { stylize() }
        icons.add(Image(Resources.getImage("icon").openStream()))
        show()
    }

}

