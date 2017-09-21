package com.godaddy.vps4.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeStamp {
    public static DateTimeFormatter hfsActionTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S").withZone(ZoneId.of("UTC"));
}
