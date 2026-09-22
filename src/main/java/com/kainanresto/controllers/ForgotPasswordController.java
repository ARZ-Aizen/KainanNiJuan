package com.kainanresto.controllers;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML private TextField targetUsernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordVisibleField;
    @FXML private Button togglePasswordBtn;
    @FXML private Button nextButton;
    @FXML private Button backToLoginButton;

    private boolean passwordVisible = false;
    private final UserDAO userDAO = new UserDAO();

    private static final String EYE_OPEN_SVG = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_CLOSED_SVG = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML public void initialize() {
        if (newPasswordVisibleField != null && newPasswordField != null) {
            newPasswordVisibleField.textProperty().bindBidirectional(newPasswordField.textProperty());
        }
    }

    @FXML private void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        newPasswordVisibleField.setManaged(passwordVisible);
        newPasswordVisibleField.setVisible(passwordVisible);
        newPasswordField.setManaged(!passwordVisible);
        newPasswordField.setVisible(!passwordVisible);

        if (togglePasswordBtn.getGraphic() instanceof SVGPath) {
            SVGPath svgPath = (SVGPath) togglePasswordBtn.getGraphic();
            svgPath.setContent(passwordVisible ? EYE_CLOSED_SVG : EYE_OPEN_SVG);
        }
    }

    @FXML private void handleNext(ActionEvent event) {
        String targetUsername = targetUsernameField.getText().trim();
        String newPassword = newPasswordField.getText();

        if (targetUsername.isEmpty() || newPassword.isEmpty()) {
            AlertUtil.showWarning("Missing Information", "Please enter both the username and the new password.");
            return;
        }

        if (newPassword.length() < 8 || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            AlertUtil.showWarning("Weak Password", "New password must be at least 8 characters long and contain both letters and numbers.");
            newPasswordField.requestFocus();
            return;
        }

        boolean userExists = userDAO.checkUserExists(targetUsername);
        if (!userExists) {
            AlertUtil.showError("User Not Found", "The username '" + targetUsername + "' does not exist or is inactive.");
            targetUsernameField.requestFocus();
            return;
        }

        openAdminAuthModal(event, targetUsername, newPassword);
    }

    private void openAdminAuthModal(ActionEvent event, String targetUsername, String newPassword) {
        Stage adminStage = new Stage();
        adminStage.initModality(Modality.APPLICATION_MODAL);
        adminStage.setTitle("Admin Authorization Required");
        adminStage.setResizable(false);

        VBox vbox = new VBox(14);
        vbox.setStyle("-fx-background-color: #F9F4EC; -fx-padding: 30;");
        vbox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Admin Approval Required");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #8C6343;");

        Label descLabel = new Label("Enter Admin/Manager credentials to approve password reset for: " + targetUsername);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #5C4B3C; -fx-font-size: 13px;");

        //STYLE PARA MATCH SA LOGIN, REG AND FORGOT PASS
        TextField adminUserField = new TextField();
        adminUserField.setPromptText("Admin Username");
        adminUserField.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C6343; -fx-prompt-text-fill: #B9926D; -fx-padding: 0; -fx-font-size: 14px;");

        HBox adminUserBox = new HBox(adminUserField);
        adminUserBox.setAlignment(Pos.CENTER_LEFT);
        adminUserBox.setStyle("-fx-background-color: #F9F4EC; -fx-border-color: #D5B392; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 15 0 15; -fx-pref-height: 50;");
        HBox.setHgrow(adminUserField, javafx.scene.layout.Priority.ALWAYS);

        //ADMIN FIELD GROUP
        PasswordField adminPassField = new PasswordField();
        adminPassField.setPromptText("Admin Password");
        adminPassField.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C6343; -fx-prompt-text-fill: #B9926D; -fx-padding: 0; -fx-font-size: 14px;");

        TextField adminPassVisibleField = new TextField();
        adminPassVisibleField.setPromptText("Admin Password");
        adminPassVisibleField.setManaged(false);
        adminPassVisibleField.setVisible(false);
        adminPassVisibleField.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C6343; -fx-prompt-text-fill: #B9926D; -fx-padding: 0; -fx-font-size: 14px;");

        adminPassVisibleField.textProperty().bindBidirectional(adminPassField.textProperty());

        StackPane passStack = new StackPane(adminPassField, adminPassVisibleField);
        passStack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passStack, javafx.scene.layout.Priority.ALWAYS);

        Button modalToggleBtn = new Button();
        modalToggleBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
        SVGPath modalToggleIcon = new SVGPath();
        modalToggleIcon.setContent(EYE_OPEN_SVG);
        modalToggleIcon.setFill(javafx.scene.paint.Color.valueOf("#8C6343"));
        modalToggleIcon.setScaleX(0.8);
        modalToggleIcon.setScaleY(0.8);
        modalToggleBtn.setGraphic(modalToggleIcon);

        boolean[] modalPassVisible = {false};
        modalToggleBtn.setOnAction(e -> {
            modalPassVisible[0] = !modalPassVisible[0];
            adminPassVisibleField.setManaged(modalPassVisible[0]);
            adminPassVisibleField.setVisible(modalPassVisible[0]);
            adminPassField.setManaged(!modalPassVisible[0]);
            adminPassField.setVisible(!modalPassVisible[0]);
            modalToggleIcon.setContent(modalPassVisible[0] ? EYE_CLOSED_SVG : EYE_OPEN_SVG);
        });

        HBox adminPassBox = new HBox(12, passStack, modalToggleBtn);
        adminPassBox.setAlignment(Pos.CENTER_LEFT);
        adminPassBox.setStyle("-fx-background-color: #F9F4EC; -fx-border-color: #D5B392; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 10 0 15; -fx-pref-height: 50;");

        Button authorizeButton = new Button("Authorize & Confirm");
        authorizeButton.setMaxWidth(Double.MAX_VALUE);
        authorizeButton.setStyle("-fx-background-color: #8C6343; -fx-text-fill: #F9F4EC; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-cursor: hand;");

        authorizeButton.setOnAction(e -> {
            String adminUser = adminUserField.getText().trim();
            String adminPass = adminPassField.getText();

            if (adminUser.isEmpty() || adminPass.isEmpty()) {
                AlertUtil.showWarning("Missing Information", "Please provide admin username and password.");
                return;
            }

            boolean authorized = userDAO.verifyAdminCredentials(adminUser, adminPass);
            if (!authorized) {
                AlertUtil.showError("Authorization Failed", "Invalid credentials or insufficient privileges. Only Admins, Managers, or Supervisors can authorize resets.");
                adminPassField.clear();
                adminPassField.requestFocus();
                return;
            }

            UserDAO.OperationResult result = userDAO.resetPassword(targetUsername, newPassword);
            if (result == UserDAO.OperationResult.SUCCESS) {
                AlertUtil.showInfo("Success", "Password successfully reset for user: " + targetUsername);
                adminStage.close();
                navigateToLogin(event);
            } else {
                AlertUtil.showError("Database Error", "An error occurred while updating the password.");
            }
        });

        vbox.getChildren().addAll(titleLabel, descLabel, adminUserBox, adminPassBox, authorizeButton);
        Scene scene = new Scene(vbox, 420, 360);
        adminStage.setScene(scene);
        adminStage.centerOnScreen();
        adminStage.showAndWait();
    }

    @FXML private void handleBackToLogin(ActionEvent event) {
        navigateToLogin(event);
    }

    private void navigateToLogin(ActionEvent event) {
        NavigationUtil.switchScene(event, "/com/kainanresto/ui/LoginView.fxml", "Kainan Ni Juan - Login");
    }
}