package com.godaddy.vps4.util;

import org.junit.Test;

public class TimestampUtilsTest {

    @Test
    public void testParseTimestamps() {
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22");
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22.3");
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22.32");
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22.456");
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22.9874");
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22.12365");
        TimestampUtils.parseHfsTimestamp("2017-10-16 20:52:22.123654");

        // No parsing exceptions means we pass.
    }

}
