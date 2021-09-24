package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineeditor.Model;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options for create (insert) a new entry
 */
public class InsertView extends InnerView {

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
        // when issues change, add them all
        model.notificator.register(Set.of(Model.Events.Issues), () -> {
            // add all issues
            choice.getItems().setAll(model.getAllIssues().stream().map(issue -> {
                // convert to an entry that will add the issue when clicked
                MenuItem menuItem = new MenuItem(issue.toString());
                menuItem.setOnAction(event -> model.createTimeEntry(issue));
                return menuItem;
            }).toList());

            // disable if no issues
            choice.setDisable(choice.getItems().isEmpty());
        });

        // when day change, enable/disable
        model.notificator.register(Set.of(Model.Events.Day), () -> {
            // disable if no date selected
            parent.setDisable(model.getDate() == null);
        });
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
        inBackground(() -> {
            // add
            model.createTimeEntries(ids);
        });
    }

}
