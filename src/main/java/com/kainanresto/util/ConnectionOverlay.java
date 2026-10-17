package com.kainanresto.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.function.Consumer;

public final class ConnectionOverlay {

    private final StackPane root;
    private final StackPane overlay = new StackPane();
    private final Label subtitle = new Label();
    private final Runnable onReconnected;
    private final Consumer<Boolean> listener;
    private final Timeline ticker;
    private long lostAtNanos;

    private ConnectionOverlay(StackPane root, Runnable onReconnected) {
        this.root = root;
        this.onReconnected = onReconnected;

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(56, 56);
        spinner.setMaxSize(56, 56);
        spinner.setStyle("-fx-progress-color: #8C6343;");

        Label title = new Label("Connection lost");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2E2520;");

        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #6E625C;");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox card = new VBox(14, spinner, title, subtitle);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setMinWidth(340);
        card.setMaxWidth(380);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 24, 0, 0, 6);");

        overlay.getChildren().add(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPickOnBounds(true);
        overlay.setFocusTraversable(true);
        overlay.setStyle("-fx-background-color: rgba(46,37,32,0.55);");
        overlay.setVisible(false);
        overlay.addEventFilter(MouseEvent.ANY, Event::consume);
        overlay.addEventFilter(KeyEvent.ANY, Event::consume);
        overlay.addEventFilter(ScrollEvent.ANY, Event::consume);

        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateSubtitle()));
        ticker.setCycleCount(Timeline.INDEFINITE);

        listener = connected -> {
            if (connected) hide(true);
            else show();
        };
    }

    public static ConnectionOverlay attach(StackPane root, Runnable onReconnected) {
        ConnectionOverlay instance = new ConnectionOverlay(root, onReconnected);
        root.getChildren().add(instance.overlay);
        ConnectionMonitor.addListener(instance.listener);
        if (!ConnectionMonitor.isConnected()) instance.show();
        return instance;
    }

    public void dispose() {
        ConnectionMonitor.removeListener(listener);
        ticker.stop();
        root.getChildren().remove(overlay);
    }

    private void show() {
        if (overlay.isVisible()) return;
        lostAtNanos = System.nanoTime();
        updateSubtitle();
        ticker.playFromStart();
        overlay.setVisible(true);
        overlay.toFront();
        overlay.requestFocus();
    }

    private void hide(boolean notify) {
        if (!overlay.isVisible()) return;
        ticker.stop();
        overlay.setVisible(false);
        if (notify && onReconnected != null) onReconnected.run();
    }

    private void updateSubtitle() {
        long seconds = (System.nanoTime() - lostAtNanos) / 1_000_000_000L;
        subtitle.setText("Reconnecting to the server... (" + seconds + "s)\n"
                + "This closes automatically once the connection is back.");
    }
}