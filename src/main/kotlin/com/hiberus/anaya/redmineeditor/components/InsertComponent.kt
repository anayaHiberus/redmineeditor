package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.enabled
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*

/**
 * Options for create (insert) a new entry
 */
internal class InsertComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var parent: Node // the parent element

    @FXML
    lateinit var choice: MenuButton // the issues choice box

    @FXML
    lateinit var input: TextField // where to paste to create issues

    @FXML
    lateinit var add: Button // the add button

    /* ------------------------- reactions ------------------------- */

    @FXML
    fun initialize() {
        // enable add button if input is not blank
        add.disableProperty().bind(input.textProperty().isEmpty) // TODO: Investigate this

        // when day issues change, add them all as menus
        AppController.onChanges(setOf(ChangeEvent.DayIssues, ChangeEvent.Assigned)) { model: Model ->

            // clear existing
            choice.items.clear()

            // add issues
            choice.items += (model.loadedIssues.also { input.enabled = it != null } ?: emptySet()) // disable input if issues are not loaded
                .map { issue ->
                    // create a menu item for each issue
                    MenuItem(issue.toShortString()).apply {
                        setOnAction {
                            // when selected, create a new entry for the issue
                            AppController.runBackground { it.createTimeEntry(issue) }
                        }
                    } to issue.project
                    // group by project
                }.groupBy { it.second }
                .map { (project, issues) ->
                    // create a menu for the project
                    Menu(project).apply {
                        // and add their issues
                        items += issues.map { it.first }
                    }
                }

            // load assigned changes if not yet
            if (model.loadedAssigned == false) {
                choice.items += MenuItem("<Load assigned>").apply {
                    setOnAction {
                        AppController.runBackground { it.loadAssigned() }
                    }
                }
            }


            // disable if no issues
            choice.enabled = choice.items.isNotEmpty()
        }

        // when day change, enable/disable
        AppController.onChanges(setOf(ChangeEvent.Day)) { model: Model ->
            // disable if no date selected
            parent.enabled = model.date != null
        }
    }

    /**
     * Add the input entries
     */
    @FXML
    private fun onAdd() {
        // get all sequential numbers
        val numbers = Regex("\\d+").findAll(input.text).map { it.value.toInt() }.toList()

        if (numbers.isEmpty()) {
            // no numbers found
            Alert(Alert.AlertType.ERROR, "No issues found, make sure you enter the identifier (sequence of numbers)").apply {
                stylize()
            }.showAndWait()
        } else {
            // create issues from them
            AppController.runBackground({ it.createTimeEntries(numbers) }) {
                input.clear()
            }
        }
    }

}