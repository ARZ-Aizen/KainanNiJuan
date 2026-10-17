package com.kainanresto.util;

import com.kainanresto.config.DatabaseConfig;
import javafx.application.Platform;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ConnectionMonitor {

    private static final long INTERVAL_SECONDS = 3;
    private static final long CHECK_TIMEOUT_SECONDS = 3;
    private static final int FAILURES_BEFORE_OFFLINE = 2;

    private static final List<Consumer<Boolean>> LISTENERS = new CopyOnWriteArrayList<>();

    private static ScheduledExecutorService scheduler;
    private static ExecutorService checker;
    private static volatile boolean connected = true;
    private static int failures = 0;

    private ConnectionMonitor() {}

    public static boolean isConnected() {
        return connected;
    }

    public static synchronized void start() {
        if (scheduler != null) return;
        connected = true;
        failures = 0;
        scheduler = Executors.newSingleThreadScheduledExecutor(daemon("connection-monitor"));
        checker = Executors.newCachedThreadPool(daemon("connection-check"));
        scheduler.scheduleWithFixedDelay(ConnectionMonitor::tick, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (checker != null) {
            checker.shutdownNow();
            checker = null;
        }
        connected = true;
        failures = 0;
    }

    public static void addListener(Consumer<Boolean> listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Consumer<Boolean> listener) {
        LISTENERS.remove(listener);
    }

    private static void tick() {
        boolean ok = probe();
        if (ok) {
            failures = 0;
            update(true);
        } else {
            failures++;
            if (failures >= FAILURES_BEFORE_OFFLINE) update(false);
        }
    }

    private static boolean probe() {
        ExecutorService exec = checker;
        if (exec == null) return connected;

        Future<Boolean> future = exec.submit(() -> {
            try (Connection c = DatabaseConfig.getConnection()) {
                return c != null && c.isValid(2);
            } catch (Exception e) {
                return false;
            }
        });

        try {
            return future.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true);
            return false;
        }
    }

    private static void update(boolean now) {
        if (connected == now) return;
        connected = now;
        Platform.runLater(() -> {
            for (Consumer<Boolean> listener : LISTENERS) {
                listener.accept(now);
            }
        });
    }

    private static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }
}