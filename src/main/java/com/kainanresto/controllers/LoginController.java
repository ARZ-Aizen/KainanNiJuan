package com.kainanresto.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        // TODO: replace with actual authentication logic
        System.out.println("Login attempt: " + username + ", rememberMe=" + rememberMe);
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // TODO: navigate to forgot-password screen
        System.out.println("Forgot password clicked");
    }

    @FXML
    private void handleRegisterHere(ActionEvent event) {
        // TODO: navigate to registration screen
        System.out.println("Register here clicked");
    }
}