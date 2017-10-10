package com.godaddy.vps4.scheduler.core.quartz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.scheduler.core.JobRequest;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class QuartzJobFactory  implements JobFactory {

    private static final Logger logger = LoggerFactory.getLogger(QuartzJobFactory.class);
    final private Injector injector;
    final private ObjectMapper objectMapper;

    @Inject
    public QuartzJobFactory(Injector injector, ObjectMapper objectMapper) {
        this.injector = injector;
        this.objectMapper = objectMapper;
    }

    private Method getSetterMethodForRequest(Class<? extends Job> jobClass,
                                             Class<? extends JobRequest> jobRequestClass)
        throws NoSuchMethodException
    {
        return jobClass.getMethod("setRequest", jobRequestClass);
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = triggerFiredBundle.getJobDetail();

        Class<? extends Job> jobClass = jobDetail.getJobClass();
        Job job = injector.getInstance(jobClass);

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String jobDataJson = jobDataMap.getString("jobDataJson");

        try {
            if (jobDataJson != null) {
                String jobRequestClassName = jobDataMap.getString("jobRequestClass");
                if (jobRequestClassName != null) {
                    @SuppressWarnings("unchecked")
                    Class<? extends JobRequest> jobRequestClass
                            = (Class<? extends JobRequest>) Class.forName(jobRequestClassName);
                    Method requestSetterMethod = getSetterMethodForRequest(jobClass, jobRequestClass);
                    if (requestSetterMethod != null) {
                        requestSetterMethod.invoke(job, objectMapper.readValue(jobDataJson, jobRequestClass));
                    }
                    else {
                        logger.error("No setter method found in job class for setting request data");
                        throw new Exception("no setter method found in job class");
                    }
                }
                else {
                    logger.error("No job request class available to deserialize job request data");
                    throw new Exception("no job request class found");
                }
            }
        }
        catch (Exception e) {
            throw new SchedulerException(e);
        }

        return job;
    }
}
