package com.hiberus.anaya.redmineeditor;

import javafx.scene.control.Alert;

public class MyException extends Exception {
    private final String title;

    public MyException(String title, String message, Exception cause) {
        super(message, cause);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void showAndWait() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(getMessage() + "\n\n" + getCause());
        alert.showAndWait();
    }

    public void merge(MyException other) {
        // TODO: better merge
        addSuppressed(other);
    }
}
