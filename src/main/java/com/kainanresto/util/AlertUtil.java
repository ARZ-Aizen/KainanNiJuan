package com.kainanresto.util;

import com.kainanresto.controllers.util.AlertController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

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
            FXMLLoader loader = new FXMLLoader(AlertUtil.class.getResource("/com/kainanresto/views/util/CustomAlert.fxml"));
            Parent root = loader.load();

            AlertController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Stage owner = findOwnerStage();
            if (owner != null) {
                stage.initOwner(owner);
            }

            controller.setStage(stage);
            controller.setAlertData(type, titleText, messageText, showSecondaryButton, primaryBtnText, secondaryBtnText);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            stage.setOpacity(0);
            stage.setOnShown(e -> {
                double x, y;

                if (owner != null && !owner.isIconified()) {
                    x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
                    y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
                } else {
                    Rectangle2D b = Screen.getPrimary().getVisualBounds();
                    x = b.getMinX() + (b.getWidth() - stage.getWidth()) / 2;
                    y = b.getMinY() + (b.getHeight() - stage.getHeight()) / 2;
                }

                stage.setX(x);
                stage.setY(y);
                stage.setOpacity(1);
            });

            stage.showAndWait();

            return controller.getResult();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Stage findOwnerStage() {
        Stage fallback = null;
        for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
            if (!(w instanceof Stage s) || !s.isShowing() || s.isIconified()) continue;
            if (s.isFocused()) return s;
            fallback = s;
        }
        return fallback;
    }
}