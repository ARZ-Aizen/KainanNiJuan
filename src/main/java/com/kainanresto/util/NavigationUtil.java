package com.kainanresto.util;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;

public final class NavigationUtil {

    private NavigationUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // PARA MAKAPAG LIPAT LIPAT NG WINDOW
    public static void switchScene(Event event, String fxmlPath, String windowTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = resolveStage(event);
            if (stage == null) {
                System.err.println("NavigationUtil: Unable to locate Stage from event source: " + event.getSource());
                return;
            }

            stage.setScene(new Scene(root));

            if (windowTitle != null && !windowTitle.isBlank()) {
                stage.setTitle(windowTitle);
            }
            configureCloseBehavior(stage, fxmlPath);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void configureCloseBehavior(Stage stage, String fxmlPath) {
        if (fxmlPath.toLowerCase().contains("loginview")) {
            stage.setOnCloseRequest(null);
            return;
        }

        stage.setOnCloseRequest((WindowEvent closeEvent) -> {
            closeEvent.consume(); // Prevent the window from abruptly exiting

            SessionManager.clearSession();

            //PARA MAPABALIK SA LOGIN
            switchScene(closeEvent, "/com/kainanresto/ui/LoginView.fxml", "Kainan Ni Juan POS - Login");
        });
    }

    private static Stage resolveStage(Event event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        if (event.getSource() instanceof Stage stage) {
            return stage;
        }
        if (event.getSource() instanceof Window window) {
            return (Stage) window;
        }
        if (event.getSource() instanceof Node node && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return null;
    }
}