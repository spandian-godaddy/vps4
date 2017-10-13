package com.godaddy.vps4.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

public class TimestampUtils {
    public static DateTimeFormatter hfsActionTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneId.of("UTC"));

    public static Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
}
