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
    private final Controller notifier; // to notify when the entry changes

    public EntryCell(Controller onChangeListener) {
        // creates new cell
        super("entry_cell.fxml");
        notifier = onChangeListener;
    }

    @Override
    public void update() {
        // sets the cell data
        TimeEntry entry = getItem();
        issue.setText(Integer.toString(entry.issue));
        comment.setText(entry.getComment());
        updateHours(entry.getHours());
    }

    // ------------------------- actions -------------------------

    @FXML
    void changedComment() {
        // update comment
        getItem().setComment(comment.getText());
    }

    @FXML
    void changeHours(Event node) {
        // update entry
        TimeEntry entry = getItem();
        entry.changeHours(Double.parseDouble(((Button) node.getTarget()).getText())); // the button label is the amount

        // update views
        updateHours(entry.getHours());
        notifier.onHourChanged();
    }

    // ------------------------- private -------------------------

    private void updateHours(double amount) {
        // set text and opacity
        hours.setText(Double.toString(amount));
        translucent.setOpacity(amount > 0 ? 1.0 : 0.5);
    }
}
