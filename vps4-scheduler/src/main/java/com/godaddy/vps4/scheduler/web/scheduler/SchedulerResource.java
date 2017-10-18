package com.godaddy.vps4.scheduler.web.scheduler;


import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.web.Vps4SchedulerApi;
import com.godaddy.vps4.scheduler.web.Vps4SchedulerException;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import io.swagger.jaxrs.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Vps4SchedulerApi
@Api(tags = {"scheduler"})
@Path("/api/scheduler")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SchedulerResource {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerResource.class);

    private final SchedulerService schedulerService;

    @Inject
    public SchedulerResource(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GET
    @Path("/{product}/{jobGroup}/jobs")
    @ApiOperation(value = "List jobs by a product's job group",
        response = SchedulerJobDetail.class,
        responseContainer = "List")
    public List<SchedulerJobDetail> getGroupJobs(
        @ApiParam(value = "The product, example 'vps4'", required = true) @PathParam("product") String product,
        @ApiParam(value = "The job group, example 'backups'", required = true) @PathParam("jobGroup") String jobGroup) {
        return schedulerService.getGroupJobs(product, jobGroup);
    }

    @POST
    @Path("/{product}/{jobGroup}/jobs")
    @ApiOperation(value = "Schedule a new job belonging to a product's job group",
        response = SchedulerJobDetail.class)
    @ApiResponses(value = {
        @ApiResponse(code = 409, message = "Could not create job, possible validation error")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(
            name = "body",
            value = "Job request data",
            required = true,
            dataTypeClass = JobRequest.class,
            paramType = "body",
            examples = @Example(value = {
                @ExampleProperty(mediaType="application/json", value="{\"when\": \"2017-10-12T23:21:04Z\"}")
            })
        )
    })
    public SchedulerJobDetail submitJobToGroup(
        @ApiParam(value = "The product, example 'vps4'", required = true) @PathParam("product") String product,
        @ApiParam(value = "The job group, example 'backups'", required = true) @PathParam("jobGroup") String jobGroup,
        @ApiParam(hidden = true) String requestJson) {
        try {
            return schedulerService.createJob(product, jobGroup, requestJson);
        }
        catch (Exception e) {
            logger.info("******** ERROR ************: {}", e.getMessage());
            throw new Vps4SchedulerException("JOB_CREATION_ERROR", "Could not create job");
        }
    }

    @GET
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    @ApiOperation(value = "Get details about a particular job",
        response = SchedulerJobDetail.class)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Job not found")
    })
    public SchedulerJobDetail getJob(
        @ApiParam(value = "The product, example 'vps4'", required = true) @PathParam("product") String product,
        @ApiParam(value = "The job group, example 'backups'", required = true) @PathParam("jobGroup") String jobGroup,
        @ApiParam(value = "The id of the job", required = true) @PathParam("jobId") UUID jobId) {
        try {
            return schedulerService.getJob(product, jobGroup, jobId);
        }
        catch (Exception e) {
            logger.info("******** ERROR ************: {}", e.getMessage());
            throw new Vps4SchedulerException("NOT_FOUND", "Could not find job");
        }
    }

    @PATCH
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    @ApiOperation(value = "Re-schedule an existing job",
        response = SchedulerJobDetail.class)
    @ApiResponses(value = {
        @ApiResponse(code = 409, message = "Could not update job, possible validation error")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "body",
                    value = "Job update data",
                    required = true,
                    dataTypeClass = JobRequest.class,
                    paramType = "body",
                    examples = @Example(value = {
                            @ExampleProperty(mediaType="application/json", value="{\"when\": \"2017-10-12T23:21:04Z\"}")
                    })
            )
    })
    public SchedulerJobDetail rescheduleJob(
        @ApiParam(value = "The product, example 'vps4'", required = true) @PathParam("product") String product,
        @ApiParam(value = "The job group, example 'backups'", required = true) @PathParam("jobGroup") String jobGroup,
        @ApiParam(value = "The id of the job", required = true) @PathParam("jobId") UUID jobId,
        @ApiParam(hidden = true) String requestJson) {
        try {
            return schedulerService.updateJobSchedule(product, jobGroup, jobId, requestJson);
        }
        catch (Exception e) {
            logger.info("******** ERROR ************: {}", e.getMessage());
            throw new Vps4SchedulerException("JOB_UPDATE_ERROR", "Couldn't update job schedule");
        }
    }

    @DELETE
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    @ApiOperation(value = "Delete an existing job")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Could not delete job, possible validation error")
    })
    public void deleteJob(
        @ApiParam(value = "The product, example 'vps4'", required = true) @PathParam("product") String product,
        @ApiParam(value = "The job group, example 'backups'", required = true) @PathParam("jobGroup") String jobGroup,
        @ApiParam(value = "The id of the job", required = true) @PathParam("jobId") UUID jobId) {
        try {
            schedulerService.deleteJob(product, jobGroup, jobId);
        }
        catch (Exception e) {
            logger.info("******** ERROR ************: {}", e.getMessage());
            throw new Vps4SchedulerException("JOB_DELETION_ERROR", "Could not delete job");
        }
    }
}
