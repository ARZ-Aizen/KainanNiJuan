package com.kainanresto.controllers;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.Role;
import com.kainanresto.model.User;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private ComboBox<String> roleComboBox;
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

    private final UserDAO userDAO = new UserDAO();

    //SVG TO
    private static final String EYE_OPEN_SVG = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_CLOSED_SVG = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML public void initialize() {
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        //PARA WALA NUMBERS SA NAME
        TextFormatter<String> nameFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[a-zA-Z\\s\\.]*$")) {
                return change;
            }
            return null;
        });
        nameField.setTextFormatter(nameFormatter);
    }

    @FXML private void handleTogglePassword() {
        passwordVisible = !passwordVisible;

        passwordVisibleField.setManaged(passwordVisible);
        passwordVisibleField.setVisible(passwordVisible);
        passwordField.setManaged(!passwordVisible);
        passwordField.setVisible(!passwordVisible);

        if (togglePasswordBtn.getGraphic() instanceof SVGPath) {
            SVGPath svgPath = (SVGPath) togglePasswordBtn.getGraphic();
            svgPath.setContent(passwordVisible ? EYE_CLOSED_SVG : EYE_OPEN_SVG);
        }
    }

    @FXML private void handleToggleConfirmPassword() {
        confirmPasswordVisible = !confirmPasswordVisible;

        confirmPasswordVisibleField.setManaged(confirmPasswordVisible);
        confirmPasswordVisibleField.setVisible(confirmPasswordVisible);
        confirmPasswordField.setManaged(!confirmPasswordVisible);
        confirmPasswordField.setVisible(!confirmPasswordVisible);

        if (toggleConfirmPasswordBtn.getGraphic() instanceof SVGPath) {
            SVGPath svgPath = (SVGPath) toggleConfirmPasswordBtn.getGraphic();
            svgPath.setContent(confirmPasswordVisible ? EYE_CLOSED_SVG : EYE_OPEN_SVG);
        }
    }

    @FXML private void handleRegister(ActionEvent event) {
        String fullName = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String roleStr = roleComboBox.getValue();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        //EMPTY CHECKING
        if (fullName.isEmpty() || username.isEmpty() || roleStr == null || password.isEmpty() || confirmPassword.isEmpty()) {
            AlertUtil.showWarning("Missing Information", "Please fill out all fields and select a role.");
            return;
        }

        //LETTER ONLY SA NAME
        if (!fullName.matches("^[a-zA-Z\\s\\.]+$")) {
            AlertUtil.showWarning("Invalid Full Name", "Full name can only contain letters and spaces.");
            nameField.requestFocus();
            return;
        }

        //ALPHANUMERIC LANG SA USERNAME AND AT LEAST 3
        if (!username.matches("^[a-zA-Z0-9_-]{3,}$")) {
            AlertUtil.showWarning("Invalid Username", "Username must be at least 3 characters long and contain only letters, numbers, underscores, or hyphens.");
            usernameField.requestFocus();
            return;
        }

        //PASSWORD STANDARD ATLEAST 8 LONG AND MAY LETTERS AND NUM
        if (password.length() < 8 || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            AlertUtil.showWarning("Weak Password", "Password must be at least 8 characters long and contain both letters and numbers.");
            passwordField.requestFocus();
            return;
        }

        //MATCH DAPAT PASS
        if (!password.equals(confirmPassword)) {
            AlertUtil.showError("Password Mismatch", "The passwords you entered do not match.");
            confirmPasswordField.requestFocus();
            return;
        }

        //MAP ROLE
        Role role = Role.fromString(roleStr);
        User newUser = new User(username, password, role, fullName, true);

        //USERDAO NA DITO
        UserDAO.OperationResult result = userDAO.register(newUser);

        switch (result) {
            case SUCCESS:
                AlertUtil.showInfo("Registration Successful", "Your account has been created successfully. You can now log in.");
                navigateToLogin(event);
                break;
            case DUPLICATE_ENTRY:
                AlertUtil.showError("Registration Failed", "The username '" + username + "' is already taken. Please choose another one.");
                break;
            case DATABASE_ERROR:
            default:
                AlertUtil.showError("Database Error", "An error occurred while connecting to the database. Please try again.");
                break;
        }
    }

    @FXML private void handleLoginLink(ActionEvent event) {
        navigateToLogin(event);
    }

    private void navigateToLogin(ActionEvent event) {
        NavigationUtil.switchScene(event, "/com/kainanresto/ui/LoginView.fxml", "Kainan Ni Juan - Login");
    }
}