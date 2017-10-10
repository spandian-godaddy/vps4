package com.godaddy.vps4.scheduler.core.utils;

public class Utils {
    public static String getJobGroupId(String product, String jobGroup) {
        return String.format("%s-%s", product, jobGroup);
    }
}
