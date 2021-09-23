package com.hiberus.anaya.redmineeditor.views;

import com.hiberus.anaya.redmineapi.Issue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

import java.util.Collection;

/**
 * Options for create (insert) a new entry
 */
public class InsertView extends InnerView {

    @FXML
    private ChoiceBox<Issue> choice; // the issues choicebox

    private final ObservableList<Issue> choiceItems = FXCollections.observableArrayList(); // items in choicebox

    @FXML
    private void initialize() {
        // init
        choice.setItems(choiceItems);

        choice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // when selected issue, add entry
            Issue issue = choice.getValue();
            if (issue == null) return;
            controller.addEntryForCurrentDate(issue);
            choice.getSelectionModel().clearSelection(); // choice.setValue(null); // TODO: how to unselect??
        });
    }

    /**
     * Display issues
     *
     * @param issues issues to display
     */
    public void setIssues(Collection<Issue> issues) {
        choiceItems.clear();
        choiceItems.addAll(issues);
    }

}
