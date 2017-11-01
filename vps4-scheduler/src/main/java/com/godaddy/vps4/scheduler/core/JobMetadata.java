package com.godaddy.vps4.scheduler.core;

import com.godaddy.vps4.scheduler.api.core.JobRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobMetadata {
    String product();
    String jobGroup();
    Class<? extends JobRequest> jobRequestType() default JobRequest.class;
}
