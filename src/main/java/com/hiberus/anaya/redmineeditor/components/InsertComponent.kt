package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*

/**
 * Options for create (insert) a new entry
 */
internal class InsertComponent : BaseComponent() {

    /* ------------------------- views ------------------------- */
    @FXML
    lateinit var choice: MenuButton // the issues choicebox

    @FXML
    lateinit var input: TextField // where to paste to create issues

    @FXML
    lateinit var add: Button // the add button

    @FXML
    lateinit var parent: Node // the parent element

    /* ------------------------- reactions ------------------------- */

    override fun init() {
        // enable add button if input is not blank
        add.disableProperty().bind(input.textProperty().isEmpty) // TODO: Investigate this

        // when issues change, add them all as menus
        controller.onChanges(setOf(ChangeEvents.Issues)) { model: Model ->

            // clear existing
            choice.items.clear()

            // add issues, grouped by project
            model.allIssues.groupBy { it.project }
                .map { (project, issues) ->
                    // create a menu for the project
                    Menu(project).apply {
                        // and add their issues
                        items += issues.map { issue ->
                            // create a menu item for each issue
                            MenuItem(issue.toShortString()).apply {
                                onAction = EventHandler {
                                    // when selected, create a new entry for an issue
                                    controller.runBackground { it.createTimeEntry(issue) }
                                }
                            }
                        }
                    }
                }.toCollection(choice.items)

            // disable if no issues
            choice.isDisable = choice.items.isEmpty()
        }

        // when day change, enable/disable
        controller.onChanges(setOf(ChangeEvents.Day)) { model: Model ->
            // disable if no date selected
            parent.isDisable = model.date == null
        }
    }

    /**
     * Add the input entries
     */
    @FXML
    private fun onAdd() {
        // get and clear text
        val content = input.text
        input.clear()

        controller.runBackground { model ->
            // create issues
            model.createTimeEntries(
                // with all sequential numbers
                Regex("\\d+").findAll(content).map { it.value.toInt() }.toList()
            )
        }
    }

}