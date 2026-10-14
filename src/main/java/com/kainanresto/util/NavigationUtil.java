package com.kainanresto.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.IOException;

public final class NavigationUtil {

    // Mabilis na transition speed (in milliseconds)
    private static final double ANIMATION_SPEED = 100.0;

    private NavigationUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // PARA MAKAPAG LIPAT LIPAT NG WINDOW
    public static void switchScene(Event event, String fxmlPath, String windowTitle) {
        Stage stage = resolveStage(event);

        if (stage == null) {
            System.err.println("NavigationUtil: Unable to locate Stage from event source: " + event.getSource());
            return;
        }

        // KUNG NAKABUKAS NA YUNG STAGE, FADE OUT ANG BUONG WINDOW HINDI LANG YUNG CONTENT
        if (stage.isShowing()) {
            Timeline popOut = new Timeline(
                    new KeyFrame(Duration.millis(ANIMATION_SPEED), new KeyValue(stage.opacityProperty(), 0.0))
            );
            popOut.setOnFinished(e -> loadAndShowNewScene(stage, fxmlPath, windowTitle));
            popOut.play();
        } else {
            // KUNG WALANG LAMAN (INITIAL LOAD), DIRETSO LOAD AGAD
            loadAndShowNewScene(stage, fxmlPath, windowTitle);
        }
    }

    private static void loadAndShowNewScene(Stage stage, String fxmlPath, String windowTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(fxmlPath));
            Parent root = loader.load();

            String pathLower = fxmlPath.toLowerCase();
            boolean isDashboard = pathLower.contains("mainadminview") || pathLower.contains("clientview");

            if (isDashboard) {
                stage.setScene(new Scene(root));
            } else {
                stage.setScene(new Scene(root, 1100, 750));
            }

            if (windowTitle != null && !windowTitle.isBlank()) {
                stage.setTitle(windowTitle);
            }

            try {
                Image icon = new Image(NavigationUtil.class.getResourceAsStream("/com/kainanresto/images/id/KainanNiJuanLogo.png"));
                stage.getIcons().add(icon);
            } catch (Exception e) {
                System.err.println("NavigationUtil: Could not load application icon - " + e.getMessage());
            }

            configureWindowMode(stage, fxmlPath);
            configureCloseBehavior(stage, fxmlPath);

            if (!isDashboard) {
                stage.sizeToScene();
            }

            stage.centerOnScreen();

            // SET OPACITY TO 0 BAGO I-SHOW PARA HINDI MAG-FLICKER
            stage.setOpacity(0.0);
            stage.show();

            // FAST FADE IN PARA SA BUONG WINDOW
            Timeline popIn = new Timeline(
                    new KeyFrame(Duration.millis(ANIMATION_SPEED), new KeyValue(stage.opacityProperty(), 1.0))
            );
            popIn.play();

        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void configureWindowMode(Stage stage, String fxmlPath) {
        String pathLower = fxmlPath.toLowerCase();
        boolean isDashboard = pathLower.contains("mainadminview") || pathLower.contains("clientview");

        if (isDashboard) {
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
        } else {
            stage.setFullScreenExitKeyCombination(KeyCombination.keyCombination("ESC"));
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(false);
        }
    }

    private static void configureCloseBehavior(Stage stage, String fxmlPath) {
        if (fxmlPath.toLowerCase().contains("loginview")) {
            stage.setOnCloseRequest(null);
            return;
        }

        stage.setOnCloseRequest((WindowEvent closeEvent) -> {
            closeEvent.consume(); // PARA DI MAGCLOSE NG TULUYAN BIGLAAN
            SessionManager.clearSession();
            // PARA MAPABALIK SA LOGIN
            switchScene(closeEvent, "/com/kainanresto/views/id/LoginView.fxml", "Kainan Ni Juan POS - Login");
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