package com.hiberus.anaya.redmineeditor.cells;

import com.hiberus.anaya.redmineeditor.Model;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class EntryCell extends SimpleListCell<Model.TimeEntries.TimeEntry> {

    // ------------------------- views -------------------------
    @FXML
    Label issue;
    @FXML
    TextField comment;
    @FXML
    Label hours;

    // ------------------------- init -------------------------

    public EntryCell() {
        super("entry_cell.fxml");
    }

    @Override
    public void update() {
        issue.setText(Integer.toString(getItem().issue));
        comment.setText(getItem().getComment());
        hours.setText(Double.toString(getItem().getHours()));
    }

    // ------------------------- actions -------------------------

    @FXML
    void changedComment() {
        // update comment
        getItem().setComment(comment.getText());
    }

    @FXML
    void changeHours(Event node) {
        // the button label is the amount
        getItem().changeHours(Double.parseDouble(((Button) node.getTarget()).getText()));
    }
}
