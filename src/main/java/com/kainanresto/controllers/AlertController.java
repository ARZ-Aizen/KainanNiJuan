package com.kainanresto.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public class AlertController {

    @FXML private VBox cardPane;
    @FXML private Circle iconCircle;
    @FXML private SVGPath iconPath;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button primaryButton;
    @FXML private Button secondaryButton;

    private boolean result = false;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean getResult() {
        return result;
    }

    public void setAlertData(String type, String title, String message, boolean showSecondary, String primaryText, String secondaryText) {
        titleLabel.setText(title);
        messageLabel.setText(message);
        primaryButton.setText(primaryText);

        if (showSecondary) {
            secondaryButton.setText(secondaryText);
            secondaryButton.setVisible(true);
            secondaryButton.setManaged(true);
        }

        if ("ERROR".equals(type)) {
            cardPane.setStyle("-fx-background-color: #FAECEC; -fx-background-radius: 16; -fx-border-color: #EAC3C3; -fx-border-radius: 16; -fx-border-width: 1;");
            iconCircle.setFill(Color.web("#B05B5B"));
            //CROSS ICON
            iconPath.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        } else if ("INFO".equals(type)) {
            //CHECK ICON
            iconPath.setContent("M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z");
        } else {
            //INFO ICON
            iconPath.setContent("M11 7h2v2h-2zm0 4h2v6h-2z M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z");
        }

        primaryButton.setOnAction(e -> {
            result = true;
            if (stage != null) stage.close();
        });

        secondaryButton.setOnAction(e -> {
            result = false;
            if (stage != null) stage.close();
        });
    }
}