package com.godaddy.vps4.scheduler.api.core.utils;

import com.godaddy.vps4.scheduler.api.core.JobGroup;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.Product;

public class Utils {
    public static String getProductForJobRequestClass(Class<? extends JobRequest> jobRequestClass) {
        return jobRequestClass.getAnnotation(Product.class).value();
    }

    public static String getJobGroupForJobRequestClass(Class<? extends JobRequest> jobRequestClass) {
        return jobRequestClass.getAnnotation(JobGroup.class).value();
    }
}
