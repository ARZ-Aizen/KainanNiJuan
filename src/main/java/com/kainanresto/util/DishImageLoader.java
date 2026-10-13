package com.kainanresto.util;

import javafx.animation.FadeTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DishImageLoader {

    private static final double QUALITY = 2.0;   // decode at 2x display size (keeps HiDPI sharp)
    private static final int MAX_ENTRIES = 150;

    private static final Map<String, Image> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public static void fadeInWhenLoaded(ImageView view, Image image) {
        view.setImage(image);

        if (image.getProgress() >= 1.0) {
            view.setOpacity(1.0);
            return;
        }

        view.setOpacity(0.0);
        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0) {
                FadeTransition ft = new FadeTransition(Duration.millis(300), view);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();
            }
        });
    }

    private DishImageLoader() {}

    public static Image load(String url, double displaySize) {
        if (url == null || url.isBlank()) return null;

        int px = (int) Math.ceil(displaySize * QUALITY);
        String key = px + "|" + url;

        Image cached = CACHE.get(key);
        if (cached != null && !cached.isError()) return cached;

        try {
            Image image = new Image(url, px, px, true, true, true);
            CACHE.put(key, image);
            return image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ImageView circleView(String url, double size) {
        Image image = load(url, size);
        if (image == null) return null;

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

    public static void applyCoverCrop(ImageView view, Image image) {
        Runnable crop = () -> {
            double w = image.getWidth(), h = image.getHeight();
            if (w <= 0 || h <= 0) return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
        };
        if (image.getProgress() >= 1.0) crop.run();
        else image.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0) crop.run();
        });
    }

    /** Call after a dish image is uploaded or replaced so stale copies aren't shown. */
    public static void clearCache() {
        CACHE.clear();
    }
}