package com.kainanresto.controllers.main;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.User;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class UserController {

    @FXML
    private AnchorPane rootpane;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (rootpane == null || rootpane.getScene() == null) {
                return;
            }

            Stage stage = (Stage) rootpane.getScene().getWindow();

            stage.setOnCloseRequest((WindowEvent event) -> {
                User currentUser = SessionManager.getCurrentUser();

                if (currentUser != null) {
                    UserDAO userDAO = new UserDAO();
                    userDAO.setAccountInactive(currentUser.getUserId());
                    System.out.println("Cashier " + currentUser.getUsername() + " status set to INACTIVE via window close.");
                }

                SessionManager.clearSession();
                NavigationUtil.switchScene(
                        event,
                        "/com/kainanresto/views/id/LoginView.fxml",
                        "Kainan Ni Juan - Login"
                );
            });
        });
    }
}