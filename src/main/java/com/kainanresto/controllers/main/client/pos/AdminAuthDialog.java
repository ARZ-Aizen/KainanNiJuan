package com.kainanresto.controllers.main.client.pos;

import com.kainanresto.dao.UserDAO;
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
        boolean[] authorized = new boolean[1];

        //HEADER TO
        Label title = new Label("Admin Authorization");
        title.getStyleClass().add("pay-title");
        Label subtitle = new Label(actionDesc);
        subtitle.getStyleClass().add("pay-subtitle");
        VBox header = new VBox(2, title, subtitle);
        header.setAlignment(Pos.CENTER);

        //CREDENTIALS
        Label errorMsg = new Label("");
        errorMsg.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
        errorMsg.setVisible(false);

        //USERNAME
        TextField userField = new TextField();
        userField.setPromptText("Admin Username");
        userField.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-prompt-text-fill: #a1887f; -fx-text-fill: #3e2723;");
        HBox userBox = new HBox(userField);
        userBox.getStyleClass().add("pay-input-box");
        userBox.setStyle("-fx-padding: 4 8 4 8;");
        HBox.setHgrow(userField, Priority.ALWAYS);
        userField.focusedProperty().addListener((o, was, is) -> userBox.pseudoClassStateChanged(FIELD_FOCUSED, is));

        //PASSWORD
        PasswordField passField = new PasswordField();
        passField.setPromptText("Admin Password");
        passField.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-prompt-text-fill: #a1887f; -fx-text-fill: #3e2723;");
        HBox passBox = new HBox(passField);
        passBox.getStyleClass().add("pay-input-box");
        passBox.setStyle("-fx-padding: 4 8 4 8;");
        HBox.setHgrow(passField, Priority.ALWAYS);
        passField.focusedProperty().addListener((o, was, is) -> passBox.pseudoClassStateChanged(FIELD_FOCUSED, is));

        VBox inputs = new VBox(12, userBox, passBox, errorMsg);

        //BUTTONS
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

        //BEHAVIOR
        Runnable doAuth = () -> {
            String uname = userField.getText().trim();
            String pwd = passField.getText();

            if (uname.isEmpty() || pwd.isEmpty()) {
                errorMsg.setText("Username and password are required.");
                errorMsg.setVisible(true);
                return;
            }

            UserDAO dao = new UserDAO();
            boolean isAuthorizedAdmin = dao.verifyAdminCredentials(uname, pwd);

            if (isAuthorizedAdmin) {
                authorized[0] = true;
                stage.close();
            } else {
                errorMsg.setText("Invalid credentials or user lacks authorization.");
                errorMsg.setVisible(true);
            }
        };

        //KEYBOARD SHORTCUT TO
        userField.setOnAction(e -> passField.requestFocus());
        passField.setOnAction(e -> doAuth.run());
        confirm.setOnAction(e -> doAuth.run());
        cancel.setOnAction(e -> stage.close());

        stage.setOnShown(e -> userField.requestFocus());

        stage.showAndWait();
        return authorized[0];
    }
}