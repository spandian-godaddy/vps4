package com.godaddy.vps4.scheduler.core.utils;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;

public class Utils {
    public static String getJobGroupId(String product, String jobGroup) {
        return String.format("%s-%s", product, jobGroup);
    }

    public static String getProductForJobClass(Class<? extends SchedulerJob> jobClass) {
        return jobClass.getAnnotation(JobMetadata.class).product();
    }

    public static String getJobGroupForJobClass(Class<? extends SchedulerJob> jobClass) {
        return jobClass.getAnnotation(JobMetadata.class).jobGroup();
    }

    public static Class<? extends JobRequest> getRequestClassForJobClass(Class<? extends SchedulerJob> jobClass) {
        return jobClass.getAnnotation(JobMetadata.class).jobRequestType();
    }

    public static String getProductForTriggerListenerClass(Class<? extends SchedulerTriggerListener> triggerListenerClass) {
        return triggerListenerClass.getAnnotation(TriggerListenerMetadata.class).product();
    }

    public static String getJobGroupForTriggerListenerClass(Class<? extends SchedulerTriggerListener> triggerListenerClass) {
        return triggerListenerClass.getAnnotation(TriggerListenerMetadata.class).jobGroup();
    }
}
