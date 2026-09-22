package com.kainanresto.util;

import com.kainanresto.controllers.AlertController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public final class AlertUtil {

    private AlertUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void showInfo(String title, String message) {
        displayCustomAlert("INFO", title, message, false, "OK", null);
    }

    public static void showError(String title, String message) {
        displayCustomAlert("ERROR", title, message, false, "OK", null);
    }

    public static void showWarning(String title, String message) {
        displayCustomAlert("WARNING", title, message, false, "OK", null);
    }

    public static boolean showYesNoConfirmation(String title, String header, String message) {
        String displayTitle = (header != null && !header.isEmpty()) ? header : title;
        return displayCustomAlert("CONFIRM", displayTitle, message, true, "Yes", "No");
    }

    public static boolean showOkCancelConfirmation(String title, String header, String message) {
        String displayTitle = (header != null && !header.isEmpty()) ? header : title;
        return displayCustomAlert("CONFIRM", displayTitle, message, true, "OK", "Cancel");
    }

    private static boolean displayCustomAlert(String type, String titleText, String messageText, boolean showSecondaryButton, String primaryBtnText, String secondaryBtnText) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertUtil.class.getResource("/com/kainanresto/ui/CustomAlert.fxml"));
            Parent root = loader.load();

            AlertController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            controller.setStage(stage);
            controller.setAlertData(type, titleText, messageText, showSecondaryButton, primaryBtnText, secondaryBtnText);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            stage.centerOnScreen();
            stage.showAndWait();

            return controller.getResult();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}