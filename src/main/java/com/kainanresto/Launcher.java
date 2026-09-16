package com.kainanresto;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/com/kainanresto/views/LoginView.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Kainan Ni Juan POS");
        stage.setScene(scene);
        stage.show();
    }
}