package com.kainanresto.controllers.main.client.pos;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.net.URL;

final class DialogSupport {

    private static final String STYLESHEET = "/com/kainanresto/styles/main/ClientView.css";

    private DialogSupport() {}

    static Stage createStage(Window owner, Region card) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        StackPane shell = new StackPane(card);
        shell.setPadding(new Insets(28));
        shell.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(shell);
        scene.setFill(Color.TRANSPARENT);
        URL css = DialogSupport.class.getResource(STYLESHEET);
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) stage.close(); });

        stage.setScene(scene);
        return stage;
    }
}