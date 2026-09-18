package com.kainanresto.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class AlertUtil {

    private AlertUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // SHOW INFO NA MESSAGE BOX

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
    }

    // SHOW ERROR NA MESSAGE BOX

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
    }

    // SHOW WARNING NA MESSAGE BOX

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
    }

    // YES OR NO NA MESSAGE BOX

    public static boolean showYesNoConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);

            ButtonType btnYes = new ButtonType("Yes");
            ButtonType btnNo = new ButtonType("No");

            alert.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnYes;
    }


    // SHOW OK BUTTON SA CANCEL NA MESSAGE BOX

    public static boolean showOkCancelConfirmation(String title, String header, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);

            Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }


}
