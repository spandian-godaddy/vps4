package com.godaddy.vps4.scheduler.core.quartz;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzJobFactory  implements JobFactory {

    private static final Logger logger = LoggerFactory.getLogger(QuartzJobFactory.class);
    final private Injector injector;
    final private ObjectMapper objectMapper;

    @Inject
    public QuartzJobFactory(Injector injector, ObjectMapper objectMapper) {
        this.injector = injector;
        this.objectMapper = objectMapper;
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {

        // get the job details
        JobDetail jobDetail = triggerFiredBundle.getJobDetail();

        // get the instance of the job class from the job details
        Class<? extends Job> jobClass = jobDetail.getJobClass();
        Job job = injector.getInstance(jobClass);

        // get the job data map associated with the job from the job details
        // the job data map holds data objects that we wish to be made available to the job when it executes
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String jobDataJson = jobDataMap.getString("jobDataJson");
        if (jobDataJson == null) {
            logger.info("Job Data not found. Creating Job Anyways.");
            return job;
        }

        // get the job request class from the job data map
        String jobRequestClassName = jobDataMap.getString("jobRequestClass");
        if (jobRequestClassName == null) {
            logger.error("Job request class not found.");
            throw new SchedulerException("Job request class not found");
        }

        try {
            // update the job instance with the job data map json
            jobClass.getMethod("setRequest", Class.forName(jobRequestClassName))
                    .invoke(job, objectMapper.readValue(jobDataJson, Class.forName(jobRequestClassName)));

        } catch(ClassNotFoundException |
                NoSuchMethodException |
                IOException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException ex) {
            throw new SchedulerException(ex);
        }

        return job;
    }
}
