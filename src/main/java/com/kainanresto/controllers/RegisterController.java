package com.kainanresto.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class RegisterController {

    @FXML private ImageView bgImageView;
    @FXML private ImageView logoImageView;

    @FXML private TextField usernameField;
    @FXML private TextField nameField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button toggleConfirmPasswordBtn;

    @FXML private Button registerButton;
    @FXML private Button loginLinkButton;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
        }
    }

    @FXML
    private void handleToggleConfirmPassword() {
        confirmPasswordVisible = !confirmPasswordVisible;
        if (confirmPasswordVisible) {
            confirmPasswordVisibleField.setText(confirmPasswordField.getText());
            confirmPasswordVisibleField.setVisible(true);
            confirmPasswordVisibleField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
        } else {
            confirmPasswordField.setText(confirmPasswordVisibleField.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordVisibleField.setVisible(false);
            confirmPasswordVisibleField.setManaged(false);
        }
    }

    @FXML
    private void handleRegister() {
        // TODO: hook up registration logic
    }

    @FXML
    private void handleLoginLink() {
        // TODO: hook up navigation to login screen
    }
}