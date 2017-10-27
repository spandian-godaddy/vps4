package com.godaddy.vps4.scheduler.core.utils;

import com.godaddy.vps4.scheduler.core.JobGroup;
import com.godaddy.vps4.scheduler.core.Product;
import com.godaddy.vps4.scheduler.core.SchedulerJob;

public class Utils {
    public static String getJobGroupId(String product, String jobGroup) {
        return String.format("%s-%s", product, jobGroup);
    }

    public static String getProductForJobClass(Class<? extends SchedulerJob> jobClass) {
        return jobClass.getAnnotation(Product.class).value();
    }

    public static String getJobGroupForJobClass(Class<? extends SchedulerJob> jobClass) {
        return jobClass.getAnnotation(JobGroup.class).value();
    }
}
