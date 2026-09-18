package com.kainanresto;

import com.kainanresto.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/com/kainanresto/ui/LoginView.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Kainan Ni Juan POS");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("Closing database connection pool...");
        DatabaseConfig.closePool();
    }
}