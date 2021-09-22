package com.hiberus.anaya.redmineeditor;

import javafx.scene.control.Alert;

import java.util.StringJoiner;

/**
 * A custom exception that allows displaying it as dialog
 */
public class MyException extends Exception {
    private final String title; // the exception title

    private final StringJoiner details = new StringJoiner("\n"); // the exception details (different from the message)

    /**
     * Creates a new exception
     *
     * @param title   with this title
     * @param message and this message
     * @param cause   and this cause
     */
    public MyException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }

    /**
     * Displays an error dialog with this exception details
     */
    public void showAndWait() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(getMessage() + "\n\n" + details);
        alert.showAndWait();
    }

    /**
     * Adds a detail whose message will be shown as a detail
     *
     * @param other another exception to add
     */
    public void addDetails(Exception other) {
        details.add(other.getMessage());
    }
}
