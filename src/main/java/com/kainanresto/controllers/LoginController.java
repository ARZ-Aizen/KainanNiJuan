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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Optional;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button forgotPasswordLink;
    @FXML private Button registerLink;
    @FXML private Button loginButton;
    private boolean passwordVisible = false;
    private final UserDAO userDAO = new UserDAO();
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
    private static final String PREF_USERNAME = "remembered_username";
    private static final String PREF_REMEMBERED = "is_remembered";

    @FXML public void initialize() {
        //PARA MAALALA UNG USERNAME
        boolean isRemembered = prefs.getBoolean(PREF_REMEMBERED, false);
        if (isRemembered) {
            String savedUsername = prefs.get(PREF_USERNAME, "");
            usernameField.setText(savedUsername);
            rememberMeCheckbox.setSelected(true);
            passwordField.requestFocus(); // Focus straight to password since username is filled
        }
    }

    @FXML private void handleTogglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setManaged(true);
            passwordVisibleField.setVisible(true);
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            setToggleIcon("/com/kainanresto/images/eye-light.png");
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            passwordVisibleField.setManaged(false);
            passwordVisibleField.setVisible(false);
            setToggleIcon("/com/kainanresto/images/eye-closed.png");
        }
    }

    private void setToggleIcon(String path) {
        Image img = new Image(getClass().getResourceAsStream(path));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(20);
        iv.setFitHeight(20);
        iv.setPreserveRatio(true);
        togglePasswordButton.setGraphic(iv);
    }

    @FXML private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordVisible
                ? passwordVisibleField.getText()
                : passwordField.getText();

        boolean rememberMe = rememberMeCheckbox != null
                && rememberMeCheckbox.isSelected();

        //VALIDATION SA LOGIN
        if (username.isEmpty() || password.isEmpty()) {
            AlertUtil.showWarning(
                    "Login Validation",
                    "Please enter both username and password."
            );
            return;
        }

        Optional<User> authenticatedUser = userDAO.login(username, password);

        if (authenticatedUser.isEmpty()) {
            AlertUtil.showError(
                    "Login Failed",
                    "Invalid username or password."
            );
            passwordField.clear();
            passwordVisibleField.clear();
            passwordField.requestFocus();
            return;
        }

        //HANDLER NG REMEMBER ME
        if (rememberMe) {
            prefs.putBoolean(PREF_REMEMBERED, true);
            prefs.put(PREF_USERNAME, username);
        } else {
            prefs.remove(PREF_REMEMBERED);
            prefs.remove(PREF_USERNAME);
        }

        User user = authenticatedUser.get();
        SessionManager.setCurrentUser(user);

        AlertUtil.showInfo(
                "Login Success",
                "Welcome back, " + user.getFullName() + "!"
        );

        navigateToDashboard(event, user);
    }

    private void navigateToDashboard(ActionEvent event, User user) {
        String fxmlPath;

        if (user.getRole() == Role.ADMIN ||
                user.getRole() == Role.MANAGER) {
            fxmlPath = "/com/kainanresto/ui/AdminView.fxml";
        } else {
            fxmlPath = "/com/kainanresto/ui/UserView.fxml";
        }

        String title =
                "Kainan Ni Juan POS - "
                        + user.getFullName()
                        + " ("
                        + user.getRole()
                        + ")";

        NavigationUtil.switchScene(
                event,
                fxmlPath,
                title
        );
    }

    @FXML private void handleForgotPassword(ActionEvent event) {
        NavigationUtil.switchScene(event, "/com/kainanresto/ui/ForgotPasswordView.fxml", "Kainan Ni Juan - Forgot Password");
    }

    @FXML private void handleRegister(ActionEvent event) {
        NavigationUtil.switchScene(event, "/com/kainanresto/ui/RegisterView.fxml", "Kainan Ni Juan - Register");
    }
}