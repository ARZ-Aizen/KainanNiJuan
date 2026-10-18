package com.kainanresto.controllers.main.admin.accounts;

import com.kainanresto.dao.UserDAO;
import com.kainanresto.model.account.User;
import com.kainanresto.util.AlertUtil;
import com.kainanresto.util.SessionManager;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class AdminAuthDialog {

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");

    private AdminAuthDialog() {}

    public static boolean show(Window owner, String actionDesc) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            AlertUtil.showError("Authorization Error", "No active session found. Please log in again.");
            return false;
        }

        boolean[] authorized = new boolean[1];

        // HEADER
        Label title = new Label("Account Authorization");
        title.getStyleClass().add("pay-title");
        Label subtitle = new Label(actionDesc);
        subtitle.getStyleClass().add("pay-subtitle");
        VBox header = new VBox(2, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // ERROR
        Label errorMsg = new Label("");
        errorMsg.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
        errorMsg.setVisible(false);

        // USERNAME - locked to the currently logged in user
        TextField userField = new TextField(currentUser.getUsername());
        userField.setEditable(false);
        userField.setFocusTraversable(false);
        userField.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-text-fill: #5d4037; -fx-font-weight: bold; -fx-padding: 6 0 6 0;");

        Label userPrefix = new Label("@");
        userPrefix.setStyle("-fx-font-size: 14px; -fx-text-fill: #a1887f; -fx-font-weight: bold; -fx-padding: 6 2 6 0;");

        Label roleBadge = new Label(currentUser.getRole() != null ? currentUser.getRole().getDisplayName() : "");
        roleBadge.setStyle("-fx-background-color: #efebe9; -fx-text-fill: #5d4037; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 12px;");

        HBox userBox = new HBox(4, userPrefix, userField, roleBadge);
        userBox.getStyleClass().add("pay-input-box");
        userBox.setStyle("-fx-padding: 6 12 6 12; -fx-background-color: #faf8f5;");
        userBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userField, Priority.ALWAYS);

        // PASSWORD
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password to authorize");
        passField.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-prompt-text-fill: #a1887f; -fx-text-fill: #3e2723; -fx-padding: 6 0 6 0;");
        HBox passBox = new HBox(passField);
        passBox.getStyleClass().add("pay-input-box");
        passBox.setStyle("-fx-padding: 6 12 6 12;");
        passBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS);
        passField.focusedProperty().addListener((o, was, is) -> passBox.pseudoClassStateChanged(FIELD_FOCUSED, is));

        VBox inputs = new VBox(12, userBox, passBox, errorMsg);

        // BUTTONS
        Button cancel = new Button("Cancel");
        cancel.setMnemonicParsing(false);
        cancel.getStyleClass().add("pay-cancel-btn");

        Button confirm = new Button("Authorize");
        confirm.setMnemonicParsing(false);
        confirm.getStyleClass().add("pay-confirm-btn");
        confirm.setDefaultButton(true);

        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(20, header, inputs, buttons);
        card.getStyleClass().add("pay-card");

        Stage stage = DialogSupport.createStage(owner, card);

        // BEHAVIOR
        Runnable doAuth = () -> {
            String uname = userField.getText().trim();
            String pwd = passField.getText();

            if (pwd.isEmpty()) {
                errorMsg.setText("Password is required.");
                errorMsg.setVisible(true);
                return;
            }

            if (!uname.equalsIgnoreCase(currentUser.getUsername())) {
                errorMsg.setText("Only the currently logged-in user can authorize this action.");
                errorMsg.setVisible(true);
                return;
            }

            UserDAO dao = new UserDAO();
            boolean isAuthorized = dao.verifyUserPassword(currentUser.getUsername(), pwd);

            if (isAuthorized) {
                authorized[0] = true;
                stage.close();
            } else {
                errorMsg.setText("Invalid password. Please try again.");
                errorMsg.setVisible(true);
                passField.selectAll();
                passField.requestFocus();
            }
        };

        passField.setOnAction(e -> doAuth.run());
        confirm.setOnAction(e -> doAuth.run());
        cancel.setOnAction(e -> stage.close());

        stage.setOnShown(e -> passField.requestFocus());

        stage.showAndWait();
        return authorized[0];
    }
}