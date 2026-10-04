package com.kainanresto.controllers.util;

import com.kainanresto.model.util.TimeSyncMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Properties;

public final class SystemTimeManager {

    private static final String CONFIG_FILE = "config.properties";

    private static final DateTimeFormatter DATE_FMT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("MMMM d, yyyy").toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.ENGLISH);

    private SystemTimeManager() {}

    public static LocalDateTime getCurrentLocalDateTime() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
        } catch (IOException ignored) {}

        boolean isAuto = Boolean.parseBoolean(props.getProperty("timeSyncAuto", "true"));
        TimeSyncMode mode = isAuto ? TimeSyncMode.AUTO : TimeSyncMode.MANUAL;

        if (mode == TimeSyncMode.AUTO) {
            return LocalDateTime.now();
        } else {
            String manualDateStr = props.getProperty("manualDate");
            String manualTimeStr = props.getProperty("manualTime");

            LocalDate date = LocalDate.now();
            LocalTime time = LocalTime.now();

            try {
                if (manualDateStr != null && !manualDateStr.isBlank()) {
                    date = LocalDate.parse(manualDateStr.replace('\u202F', ' ').trim(), DATE_FMT);
                }
            } catch (Exception ignored) {}

            try {
                if (manualTimeStr != null && !manualTimeStr.isBlank()) {
                    time = LocalTime.parse(manualTimeStr.replace('\u202F', ' ').trim(), TIME_FMT);
                }
            } catch (Exception ignored) {}

            return LocalDateTime.of(date, time);
        }
    }
}