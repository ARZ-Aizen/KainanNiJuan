package com.kainanresto.controllers.id;

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
import javafx.scene.shape.SVGPath;

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

    // SVG EYE ICON
    private static final String EYE_OPEN_SVG = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_CLOSED_SVG = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML public void initialize() {
        boolean isRemembered = prefs.getBoolean(PREF_REMEMBERED, false);
        if (isRemembered) {
            String savedUsername = prefs.get(PREF_USERNAME, "");
            usernameField.setText(savedUsername);
            rememberMeCheckbox.setSelected(true);
            passwordField.requestFocus();
        }
    }

    @FXML private void handleTogglePassword() {
        passwordVisible = !passwordVisible;

        passwordVisibleField.setManaged(passwordVisible);
        passwordVisibleField.setVisible(passwordVisible);
        passwordField.setManaged(!passwordVisible);
        passwordField.setVisible(!passwordVisible);

        if (togglePasswordButton.getGraphic() instanceof SVGPath) {
            SVGPath svgPath = (SVGPath) togglePasswordButton.getGraphic();
            svgPath.setContent(passwordVisible ? EYE_CLOSED_SVG : EYE_OPEN_SVG);
        }
    }

    @FXML private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordVisible
                ? passwordVisibleField.getText()
                : passwordField.getText();

        boolean rememberMe = rememberMeCheckbox != null
                && rememberMeCheckbox.isSelected();

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
            fxmlPath = "/com/kainanresto/views/main/AdminView.fxml";
        } else {
            fxmlPath = "/com/kainanresto/views/main/UserView.fxml";
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
        NavigationUtil.switchScene(event, "/com/kainanresto/views/id/ForgotPasswordView.fxml", "Kainan Ni Juan - Forgot Password");
    }

    @FXML private void handleRegister(ActionEvent event) {
        NavigationUtil.switchScene(event, "/com/kainanresto/views/id/RegisterView.fxml", "Kainan Ni Juan - Register");
    }

    // SA SHORTCUT KEY LANG TO
    @FXML private void handleUsernameEnter(ActionEvent event) {
        if (passwordVisible) {
            passwordVisibleField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
    }

}