package com.hiberus.anaya.redmineeditor.controllers;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class EntryCell extends SimpleListCell<Model.TimeEntries.TimeEntry> {
    @FXML
    Label hours;
    @FXML
    Label issue;

    @FXML
    TextField comment;

    public EntryCell() {
        super("entry_cell.fxml");
    }

    @Override
    public void update() {
        hours.setText(Double.toString(getItem().getHours()));
        issue.setText(Integer.toString(getItem().issue));
        comment.setText(getItem().getComment());
    }

    @FXML
    void changedComment() {
        getItem().setComment(comment.getText());
    }

    @FXML
    void changeHours(Event node) {
        getItem().changeHours(Double.parseDouble(((Button) node.getTarget()).getText()));
    }
}
