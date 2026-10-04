package com.kainanresto.model.util;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppSettings(String restaurantName, String contactEmail, String logoUri,
                          TimeSyncMode timeSyncMode, LocalDate systemDate, LocalTime systemTime,
                          String language, boolean autoBackupEnabled,
                          boolean requirePinForSensitiveActions) {}