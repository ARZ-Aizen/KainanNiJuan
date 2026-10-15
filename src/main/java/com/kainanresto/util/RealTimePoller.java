package com.kainanresto.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class RealTimePoller {
    private static Timeline pollingTimeline;
    private static Runnable activeListener;

    public static void startPolling(Runnable onRefreshTick, double secondsInterval) {
        stopPolling();
        activeListener = onRefreshTick;

        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(secondsInterval), event -> {
            if (activeListener != null) {
                activeListener.run();
            }
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    public static void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
        activeListener = null;
    }
}