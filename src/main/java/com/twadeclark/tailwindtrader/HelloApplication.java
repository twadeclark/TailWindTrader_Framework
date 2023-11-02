package com.twadeclark.tailwindtrader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.image.Image;

import javax.swing.*;
import java.io.IOException;

public class HelloApplication extends Application {
    private HelloController controller;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Image icon = new Image("TailWindLogo.jpg");
            stage.getIcons().add(icon);

            stage.setTitle("TailWind Trader");
            scene.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
            stage.setScene(scene);

            controller = fxmlLoader.getController();

            stage.setOnCloseRequest(event -> {
                try {
                    stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            stage.show();
        } catch (IOException e) {
            JOptionPane.showConfirmDialog(null, e.toString(), "Big Problems", JOptionPane.ERROR_MESSAGE);

            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        controller.shutdownSequence();
    }

}