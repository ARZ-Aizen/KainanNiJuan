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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Optional;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private Button loginButton;

    private boolean passwordVisible = false;

    private final UserDAO userDAO = new UserDAO();


    @FXML
    private void handleTogglePassword() {

        passwordVisible = !passwordVisible;

        if (passwordVisible) {

            // Copy password to visible field
            passwordVisibleField.setText(passwordField.getText());

            // Show visible text field
            passwordVisibleField.setManaged(true);
            passwordVisibleField.setVisible(true);

            // Hide password field
            passwordField.setManaged(false);
            passwordField.setVisible(false);

            // Changed the icon into seeing
            setToggleIcon("/com/kainanresto/images/eye-light.png");

        } else {

            // Copy password back to password field
            passwordField.setText(passwordVisibleField.getText());

            // Show password field
            passwordField.setManaged(true);
            passwordField.setVisible(true);

            // Hide visible field
            passwordVisibleField.setManaged(false);
            passwordVisibleField.setVisible(false);

            // Changed the icon into closed eye
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

    @FXML
    private void handleLogin(ActionEvent event) {

        String username = usernameField.getText().trim();

        // Get password from whichever field is currently visible
        String password = passwordVisible
                ? passwordVisibleField.getText()
                : passwordField.getText();

        boolean rememberMe = rememberMeCheckBox != null
                && rememberMeCheckBox.isSelected();



        // Login Validation
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

        User user = authenticatedUser.get();

        SessionManager.setCurrentUser(user);

        AlertUtil.showInfo(
                "Login Success",
                "Welcome back, " + user.getFullName() + "!"
        );

        if (rememberMe) {

            System.out.println(
                    "Remember Me flag activated for: " + username
            );

            // You can add actual Remember Me storage here later.
        }

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

    // Forgot Password (No logic)
    @FXML
    private void handleForgotPassword(ActionEvent event) {

        System.out.println("Forgot password clicked");

        // Logic
    }

    // Register (No logic)
    @FXML
    private void handleRegister(ActionEvent event) {

        System.out.println("Register here clicked");

        // Logic
    }
}
