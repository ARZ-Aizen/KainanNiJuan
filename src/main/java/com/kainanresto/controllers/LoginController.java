package com.kainanresto.controllers;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.Role;
import com.kainanresto.model.User;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.NavigationUtil;
import com.kainanresto.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

        // PANGCHECK IF EMPTY
        if (username.isEmpty() || password.isEmpty()) {
            AlertUtil.showWarning("Login Validation", "Please enter both username and password.");
            return;
        }

        // PANG DOUBLE CHECK PARA SA LOGIN
        Optional<User> authenticatedUser = userDAO.login(username, password);

        if (authenticatedUser.isEmpty()) {
            AlertUtil.showError("Login Failed", "Invalid username or password.");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }

        User user = authenticatedUser.get();

        // 1. SAVE USER TO ACTIVE RAM SESSION
        SessionManager.setCurrentUser(user);

        AlertUtil.showInfo("Login Success", "Welcome back, " + user.getFullName() + "!");

        // REMEMBER ME LOGIC
        if (rememberMe) {
            System.out.println("Remember Me flag activated for: " + username);
        }

        // 2. NAVIGATE IN ONE LINE
        navigateToDashboard(event, user);
    }

    private void navigateToDashboard(ActionEvent event, User user) {
        String fxmlPath = (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                ? "/com/kainanresto/ui/AdminView.fxml"
                : "/com/        kainanresto/ui/UserView.fxml";

        String title = "Kainan Ni Juan POS - " + user.getFullName() + " (" + user.getRole() + ")";

        NavigationUtil.switchScene(event, fxmlPath, title);
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