package com.hiberus.anaya.redmineeditor.components;

import com.hiberus.anaya.redmineapi.Issue;
import com.hiberus.anaya.redmineeditor.model.ChangeEvents;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options for create (insert) a new entry
 */
public class InsertComponent extends BaseComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    private MenuButton choice; // the issues choicebox
    @FXML
    private TextField input; // where to paste to create issues
    @FXML
    public Button add; // the add button
    @FXML
    private Node parent; // the parent element

    /* ------------------------- reactions ------------------------- */


    @Override
    void init() {
        // when issues change, add them all as menus
        controller.register(Set.of(ChangeEvents.Issues), model -> {
            // clear existing
            choice.getItems().clear();
            Map<String, Menu> projects = new HashMap<>(); // cache for menu entries

            // add each entry
            for (Issue issue : model.getAllIssues()) {
                // convert to an entry that will add the issue when clicked
                MenuItem menuItem = new MenuItem(issue.toShortString());
                menuItem.setOnAction(event -> createEntry(issue));

                String project = issue.project;
                if (!projects.containsKey(project)) {
                    // create menu if not existing
                    Menu newProject = new Menu(project);
                    projects.put(project, newProject);
                    choice.getItems().add(newProject);
                }

                // add to menu
                projects.get(project).getItems().add(menuItem);
            }

            // disable if no issues
            choice.setDisable(choice.getItems().isEmpty());
        });

        // when day change, enable/disable
        controller.register(Set.of(ChangeEvents.Day), model -> {
            // disable if no date selected
            parent.setDisable(model.getDate() == null);
        });
    }

    private void createEntry(Issue issue) {
        // create a new entry for an issue
        controller.runBackground(model -> model.createTimeEntry(issue));
    }

    @FXML
    private void onInputKey(KeyEvent event) {
        // enable add button if input is not blank
        add.setDisable(input.getText().isBlank());

        // when pressing enter, add
        if (event.getCode() == KeyCode.ENTER) onAdd();
    }

    @FXML
    private void onAdd() {
        // add the entered entries

        // get and clear text
        String content = input.getText();
        input.clear();

        // extract all sequential numbers
        Matcher m = Pattern.compile("\\d+").matcher(content);
        List<Integer> ids = new ArrayList<>();
        while (m.find()) {
            ids.add(Integer.parseInt(m.group(0)));
        }

        // and add them
        controller.runBackground(model -> {
            model.createTimeEntries(ids);
        });
    }

}
