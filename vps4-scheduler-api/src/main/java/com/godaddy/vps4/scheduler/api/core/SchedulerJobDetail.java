package com.godaddy.vps4.scheduler.api.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.time.Instant;
import java.util.UUID;

public class SchedulerJobDetail {
    public final UUID id;

    @ApiModelProperty(
        value = "This is an ISO 8601 formatted date string",
        example = "2018-01-24T16:52:55Z",
        dataType = "java.lang.String")
    public final Instant nextRun;

    public final JobRequest jobRequest;

    @JsonCreator
    public SchedulerJobDetail(@JsonProperty("id") UUID id,
                              @JsonProperty("nextRun") Instant nextRun,
                              @JsonProperty("jobRequest") JobRequest jobRequest) {
        this.id = id;
        this.nextRun = nextRun;
        this.jobRequest = jobRequest;
    }

    @Override
    public String toString() {
        return String.format("Scheduler job detail [id=%s, nextRun=%s, jobRequest=%s]", id, nextRun, jobRequest.toString());
    }
}
