package com.game.utils;

import java.time.*;

public class TimeUtils {
    private static final ZoneId ZONE_ID = ZoneId.of("UTC+7");

    public static LocalDateTime parseWithTimezone(String timestamp) {
        return LocalDateTime.parse(timestamp).atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZONE_ID)
                .toLocalDateTime();
    }

    public static int getMinuteOfDay(LocalDateTime dateTime) {
        return dateTime.getHour() * 60 + dateTime.getMinute();
    }

    public static LocalDate getDate(LocalDateTime dateTime) {
        return dateTime.toLocalDate();
    }
}