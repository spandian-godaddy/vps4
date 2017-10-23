package com.godaddy.vps4.scheduler.web.client;

import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.core.SchedulerJobDetail;
import io.swagger.jaxrs.PATCH;

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

@Path("/api/scheduler")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SchedulerService {
    @GET
    @Path("/{product}/{jobGroup}/jobs")
    List<SchedulerJobDetail> getGroupJobs(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup);

    @POST
    @Path("/{product}/{jobGroup}/jobs")
    SchedulerJobDetail submitJobToGroup(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, JobRequest requestJson);

    @GET
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    SchedulerJobDetail getJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId);

    @PATCH
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    SchedulerJobDetail rescheduleJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId, JobRequest requestJson);

    @DELETE
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    void deleteJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId);
}
