package com.kainanresto;

import com.kainanresto.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/kainanresto/views/id/LoginView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);
        stage.setTitle("Kainan Ni Juan - Login");
        stage.setResizable(false);

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/kainanresto/images/id/KainanNiJuan.png")));
        } catch (Exception e) {
            System.err.println("Launcher: Could not load application icon - " + e.getMessage());
        }

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("Closing database connection pool...");
        DatabaseConfig.closePool();
    }
}