package com.kainanresto.controllers.main.client;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

import java.math.BigDecimal;
import java.util.Locale;

public class ClientUIHelper {

    public static void fixSize(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    public static void applyCoverCrop(ImageView view, Image image) {
        Runnable crop = () -> {
            double w = image.getWidth();
            double h = image.getHeight();
            if (w <= 0.0 || h <= 0.0) return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2.0, (h - side) / 2.0, side, side));
        };
        if (image.getProgress() >= 1.0) crop.run();
        else image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) crop.run();
        });
    }

    public static String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "\u2014" : value;
    }

    public static String formatPeso(BigDecimal amount) {
        return amount == null ? "\u2014" : String.format(Locale.ENGLISH, "\u20B1 %,.2f", amount);
    }

    public static void fitGridIcon(SVGPath icon, double targetSize, double strokePx) {
        if (icon == null) return;
        double scale = targetSize / 24.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-stroke-width: " + (strokePx / scale) + ";");
    }
}