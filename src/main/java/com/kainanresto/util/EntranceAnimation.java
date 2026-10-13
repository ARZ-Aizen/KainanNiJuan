package com.kainanresto.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class EntranceAnimation {

    private static final Duration DURATION = Duration.millis(450);

    private static final double SIDEBAR_OFFSET_X = -40;
    private static final double TOPBAR_OFFSET_Y  = -20;
    private static final double CONTENT_OFFSET_Y = 24;

    private static final int TOPBAR_DELAY_MS  = 120;
    private static final int CONTENT_DELAY_MS = 200;

    private EntranceAnimation() {}

    public static void prepare(Node sidebar, Node topbar, Node content) {
        hide(sidebar, SIDEBAR_OFFSET_X, 0);
        hide(topbar, 0, TOPBAR_OFFSET_Y);
        hide(content, 0, CONTENT_OFFSET_Y);
    }

    public static void play(Node sidebar, Node topbar, Node content) {
        ParallelTransition all = new ParallelTransition();

        add(all, sidebar, SIDEBAR_OFFSET_X, 0, 0);
        add(all, topbar, 0, TOPBAR_OFFSET_Y, TOPBAR_DELAY_MS);
        add(all, content, 0, CONTENT_OFFSET_Y, CONTENT_DELAY_MS);

        all.play();
    }

    private static void hide(Node node, double fromX, double fromY) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateX(fromX);
        node.setTranslateY(fromY);
    }

    private static void add(ParallelTransition all, Node node,
                            double fromX, double fromY, int delayMs) {
        if (node == null) return;

        FadeTransition fade = new FadeTransition(DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(DURATION, node);
        slide.setFromX(fromX);
        slide.setFromY(fromY);
        slide.setToX(0);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition group = new ParallelTransition(fade, slide);
        group.setDelay(Duration.millis(delayMs));
        all.getChildren().add(group);
    }
}