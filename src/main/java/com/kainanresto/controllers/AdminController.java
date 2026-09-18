package com.kainanresto.controllers;

import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class AdminController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent closeEvent) -> {
                closeEvent.consume();
                SessionManager.clearSession();
                NavigationUtil.switchScene(
                        closeEvent,
                        "/com/kainanresto/ui/LoginView.fxml",
                        "Kainan Ni Juan POS - Login"
                );
            });
        });
    }
}