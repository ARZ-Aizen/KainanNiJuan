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
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class AdminAuthDialog {

    private static final PseudoClass FIELD_FOCUSED = PseudoClass.getPseudoClass("field-focused");
    private static final String EYE_OPEN_SVG = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_CLOSED_SVG = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

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

        // PASSWORD with Eye Toggle
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password to authorize");
        passField.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-prompt-text-fill: #a1887f; -fx-text-fill: #3e2723; -fx-padding: 6 0 6 0;");

        TextField plainField = new TextField();
        plainField.setPromptText("Enter your password to authorize");
        plainField.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-prompt-text-fill: #a1887f; -fx-text-fill: #3e2723; -fx-padding: 6 0 6 0;");
        plainField.setVisible(false);
        plainField.setManaged(false);

        passField.textProperty().bindBidirectional(plainField.textProperty());

        boolean[] showPassword = new boolean[]{false};
        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent(EYE_OPEN_SVG);
        eyeIcon.setFill(Color.web("#a1887f"));
        eyeIcon.setScaleX(16.0 / 24.0);
        eyeIcon.setScaleY(16.0 / 24.0);

        Button eyeBtn = new Button();
        eyeBtn.setGraphic(eyeIcon);
        eyeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6;");
        eyeBtn.setFocusTraversable(false);
        eyeBtn.setOnMouseEntered(e -> eyeIcon.setFill(Color.web("#5d4037")));
        eyeBtn.setOnMouseExited(e -> eyeIcon.setFill(Color.web("#a1887f")));

        eyeBtn.setOnAction(e -> {
            showPassword[0] = !showPassword[0];
            plainField.setVisible(showPassword[0]);
            plainField.setManaged(showPassword[0]);
            passField.setVisible(!showPassword[0]);
            passField.setManaged(!showPassword[0]);

            eyeIcon.setContent(showPassword[0] ? EYE_CLOSED_SVG : EYE_OPEN_SVG);

            if (showPassword[0]) {
                plainField.requestFocus();
                plainField.positionCaret(plainField.getText().length());
            } else {
                passField.requestFocus();
                passField.positionCaret(passField.getText().length());
            }
        });

        HBox passBox = new HBox(passField, plainField, eyeBtn);
        passBox.getStyleClass().add("pay-input-box");
        passBox.setStyle("-fx-padding: 6 12 6 12;");
        passBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS);
        HBox.setHgrow(plainField, Priority.ALWAYS);

        passField.focusedProperty().addListener((o, was, is) -> passBox.pseudoClassStateChanged(FIELD_FOCUSED, is));
        plainField.focusedProperty().addListener((o, was, is) -> passBox.pseudoClassStateChanged(FIELD_FOCUSED, is));

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
                if (showPassword[0]) {
                    plainField.selectAll();
                    plainField.requestFocus();
                } else {
                    passField.selectAll();
                    passField.requestFocus();
                }
            }
        };

        passField.setOnAction(e -> doAuth.run());
        plainField.setOnAction(e -> doAuth.run());
        confirm.setOnAction(e -> doAuth.run());
        cancel.setOnAction(e -> stage.close());

        stage.setOnShown(e -> passField.requestFocus());

        stage.showAndWait();
        return authorized[0];
    }
}