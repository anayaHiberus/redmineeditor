package com.hiberus.anaya.redmineeditor.cells;

import com.hiberus.anaya.redmineapi.TimeEntry;
import com.hiberus.anaya.redmineeditor.Controller;
import com.hiberus.anaya.redmineeditor.utils.SimpleListCell;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * One of the entries in the entries list
 */
public class EntryCell extends SimpleListCell<TimeEntry> {

    // ------------------------- views -------------------------
    @FXML
    HBox translucent;
    @FXML
    Label issue;
    @FXML
    TextField comment;
    @FXML
    Label hours;

    // ------------------------- init -------------------------
    private final Controller notifier;

    public EntryCell(Controller onChangeListener) {
        super("entry_cell.fxml");
        notifier = onChangeListener;
    }

    @Override
    public void update() {
        issue.setText(Integer.toString(getItem().issue));
        comment.setText(getItem().getComment());
        hours.setText(Double.toString(getItem().getHours()));
        translucent.setOpacity(getItem().getHours() > 0 ? 1.0 : 0.5);
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
        hours.setText(Double.toString(getItem().getHours()));

        notifier.onHourChanged();
    }
}
