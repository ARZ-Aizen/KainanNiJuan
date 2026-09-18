package com.kainanresto.controllers;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.Role;
import com.kainanresto.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.kainanresto.util.AlertUtil;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Hyperlink registerHereLink;

    @FXML
    private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        // 1. Basic UI-level sanity check
        if (username.isEmpty() || password.isEmpty()) {
            AlertUtil.showWarning("Login Validation", "Please enter both username and password.");
            return;
        }

        // 2. Delegate authentication to UserDAO
        Optional<User> authenticatedUser = userDAO.login(username, password);

        if (authenticatedUser.isEmpty()) {
            AlertUtil.showError("Login Failed", "Invalid username or password.");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }

        User user = authenticatedUser.get();
        AlertUtil.showInfo("Login Success", "Welcome back, " + user.getFullName() + "!");

        // 3. Optional Remember Me logic handling
        if (rememberMe) {
            System.out.println("Remember Me flag activated for: " + username);
        }

        navigateToDashboard(event, user);
    }

    private void navigateToDashboard(ActionEvent event, User user) {
        String fxmlPath;

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            fxmlPath = "/com.kainanresto/ui/AdminView.fxml";
        } else {
            // Default cashier/staff landing screen
            fxmlPath = "/com.kainanresto/ui/UserView.fxml";
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Kainan Ni Juan POS - " + user.getFullName() + " (" + user.getRole() + ")");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Could not load the destination view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        System.out.println("Forgot password clicked");
    }

    @FXML
    private void handleRegisterHere(ActionEvent event) {
        System.out.println("Register here clicked");
    }
}