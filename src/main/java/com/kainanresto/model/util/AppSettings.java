package com.kainanresto.model.util;

import java.time.LocalDate;
import java.time.LocalTime;
/**
 * systemDate / systemTime are only meaningful when timeSyncMode == MANUAL (null in AUTO).
 * logoUri is any URI Image can load (file:, http:, ...), or null for "no logo".
 */
public record AppSettings(String restaurantName, String contactEmail, String logoUri,
                          TimeSyncMode timeSyncMode, LocalDate systemDate, LocalTime systemTime,
                          String language, boolean autoBackupEnabled,
                          boolean requirePinForSensitiveActions) {}