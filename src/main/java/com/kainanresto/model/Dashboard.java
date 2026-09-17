package com.kainanresto.model;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Dashboard extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/kainanresto/views/Dashboard.fxml"));
        stage.setScene(new Scene(root, 1024, 768));
        stage.setTitle("Dashboard");
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}