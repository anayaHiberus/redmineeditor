package com.hiberus.anaya.redmineeditor;

import javafx.scene.control.Alert;

import java.util.StringJoiner;

public class MyException extends Exception {
    private final String title;

    private final StringJoiner details = new StringJoiner("\n");

    public MyException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }

    public void showAndWait() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(getMessage() + "\n\n" + details);
        alert.showAndWait();
    }

    public void addDetails(Exception other) {
        details.add(other.getMessage());
    }
}
