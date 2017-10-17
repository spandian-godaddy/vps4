package com.godaddy.vps4.util;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

public class TimestampUtils {
    public static Instant parseHfsTimestamp(String hfsTimestamp) {
        return Instant.parse(hfsTimestamp.replace(' ', 'T').concat("Z"));
    }

    public static Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
}