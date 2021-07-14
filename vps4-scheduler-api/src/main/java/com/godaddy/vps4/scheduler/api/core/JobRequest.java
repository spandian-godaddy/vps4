package com.godaddy.vps4.scheduler.api.core;

import java.time.Instant;

import io.swagger.annotations.ApiModelProperty;

public class JobRequest extends JobRequestBase {

    public static final int JOB_SCHEDULE_LEAD_TIME_WINDOW = 60; // 60 seconds

    @ApiModelProperty(
        value = "This is an ISO 8601 formatted date string",
        dataType = "java.lang.String",
        example = "2018-01-24T16:52:55Z",
        required = true)
    @Required
    public Instant when;

    @Required public JobType jobType;
    @Optional public Integer repeatCount;
    @Optional public Integer repeatIntervalInDays;

    protected void validate() throws Exception {
        super.validate();
        if(jobType.equals(JobType.RECURRING) && repeatIntervalInDays == null) {
            throw new Vps4JobRequestValidationException(
                "INVALID_REPEAT_INTERVAL",
                "Repeat interval is required for a recurring job");
        }
    }

    private void validateWhen() throws Vps4JobRequestValidationException {
        if(!when.isAfter(Instant.now().plusSeconds(JOB_SCHEDULE_LEAD_TIME_WINDOW))) {
            throw new Vps4JobRequestValidationException(
                "INVALID_START_TIME",
                String.format(
                    "Job can be scheduled to run only after: %s",
                    Instant.now().plusSeconds(JOB_SCHEDULE_LEAD_TIME_WINDOW)));
        }
    }

    private void validateRepeatCount() throws Vps4JobRequestValidationException {
        if (jobType.equals(JobType.RECURRING) && repeatCount < 1) {
            throw new Vps4JobRequestValidationException("INVALID_REPEAT_COUNT", "Repeat count should be a positive integer");
        }
    }

    private void validateRepeatIntervalInDays() throws Vps4JobRequestValidationException {
        if(jobType.equals(JobType.RECURRING) && repeatIntervalInDays < 1) {
            throw new Vps4JobRequestValidationException(
                "INVALID_REPEAT_INTERVAL",
                "Repeat interval is required for a recurring job and should needs be a positive integer value");
        }
    }
}
