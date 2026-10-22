package com.kainanresto.util;

import com.kainanresto.dao.ProductDAO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DishImageLoader {

    private static final double QUALITY = 2.0;
    private static final int MAX_ENTRIES = 200;

    private static final Map<String, Image> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    private DishImageLoader() {
    }

    public static Image load(String url, double displaySize) {
        if (url == null || url.isBlank())
            return null;

        String sanitizedUrl = url.trim().replace('\\', '/');
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://") && !sanitizedUrl.startsWith("file:")) {
            sanitizedUrl = ProductDAO.resolveImageUrl(sanitizedUrl, null);
            if (sanitizedUrl == null || sanitizedUrl.isBlank())
                return null;
        }

        if (sanitizedUrl.startsWith("http://") || sanitizedUrl.startsWith("https://") || sanitizedUrl.startsWith("file:")) {
            sanitizedUrl = sanitizedUrl.replace(" ", "%20");
        }

        int px = (int) Math.ceil(displaySize * QUALITY);
        String key = px + "|" + sanitizedUrl;

        Image cached = CACHE.get(key);
        if (cached != null && !cached.isError())
            return cached;

        final String finalUrl = sanitizedUrl;
        try {
            Image image = new Image(finalUrl, px, px, true, true, true);
            image.errorProperty().addListener((obs, oldV, isErr) -> {
                if (Boolean.TRUE.equals(isErr)) {
                    CACHE.remove(key);
                    System.err.println("DishImageLoader: Failed to load image from " + finalUrl + ": "
                            + (image.getException() != null ? image.getException().getMessage() : "unknown error"));
                }
            });
            CACHE.put(key, image);
            return image;
        } catch (IllegalArgumentException e) {
            System.err.println("DishImageLoader: Could not load image from '" + finalUrl + "': " + e.getMessage());
            return null;
        }
    }

    public static Node circleView(String url, double size) {
        Image image = load(url, size);
        if (image == null) {
            return createPlaceholder(size);
        }

        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        applyCoverCrop(view, image);
        view.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        return view;
    }

    public static Node createPlaceholder(double size) {
        StackPane pane = new StackPane();
        pane.setPrefSize(size, size);
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        pane.setStyle("-fx-background-color: #F4EBE1; -fx-background-radius: 999px; -fx-alignment: center;");

        // Elegant restaurant utensil / dish SVG icon
        SVGPath icon = new SVGPath();
        icon.setContent("M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7zm5-3v8h2.5v8H21V2c-2.76 0-5 2.24-5 4z");
        icon.setFill(Color.web("#B8A598"));

        double scale = (size * 0.38) / 24.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);

        pane.getChildren().add(icon);
        pane.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        return pane;
    }

    public static void fadeInWhenLoaded(ImageView view, Image image) {
        if (image == null) return;
        view.setImage(image);

        if (image.getProgress() >= 1.0) {
            view.setOpacity(1.0);
            return;
        }

        view.setOpacity(0.0);
        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0) {
                Platform.runLater(() -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(250), view);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.play();
                });
            }
        });
    }

    public static void applyCoverCrop(ImageView view, Image image) {
        if (image == null) return;

        Runnable action = () -> {
            double w = image.getWidth(), h = image.getHeight();
            if (w <= 0 || h <= 0)
                return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
        };

        if (image.getProgress() >= 1.0) {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } else {
            image.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0) {
                    Platform.runLater(action);
                }
            });
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}