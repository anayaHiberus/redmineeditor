package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineapi.Issue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options for create (insert) a new entry
 */
public class InsertView extends InnerView {

    // ------------------------- views -------------------------

    @FXML
    private MenuButton choice; // the issues choicebox

    @FXML
    private TextField input; // where to paste to create issues
    @FXML
    public Button add; // the add button

    @FXML
    private Node parent; // the parent element

    // ------------------------- reactions -------------------------

    @FXML
    private void onInputKey(KeyEvent event) {
        // change add enabled state
        add.setDisable(input.getText().isBlank());

        // when pressing enter, add
        if (event.getCode() == KeyCode.ENTER) onAdd();
    }

    @FXML
    private void onAdd() {
        // add the searchable entries

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
        controller.addEntriesForCurrentDate(ids);
    }

    // ------------------------- actions -------------------------

    /**
     * Display issues
     *
     * @param issues issues to display
     */
    public void setIssues(Collection<Issue> issues) {
        choice.getItems().setAll(issues.stream().map(issue -> {
            // convert to an entry that will add the issue when clicked
            MenuItem menuItem = new MenuItem(issue.toString());
            menuItem.setOnAction(event -> controller.addEntryForCurrentDate(issue));
            return menuItem;
        }).toList());
        // disable if no entries
        choice.setDisable(choice.getItems().isEmpty());
    }

    /**
     * @param enable new enabled state
     */
    public void setEnabled(boolean enable) {
        parent.setDisable(!enable);
    }
}
