package com.godaddy.vps4.scheduler.api.web;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/scheduler")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SchedulerWebService {
    @GET
    @Path("/{product}/{jobGroup}/jobs")
    List<SchedulerJobDetail> getGroupJobs(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup) throws WebApplicationException;

    @POST
    @Path("/{product}/{jobGroup}/jobs")
    SchedulerJobDetail submitJobToGroup(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, JobRequest requestJson) throws WebApplicationException;

    @POST
    @Path("/{product}/{jobGroup}/jobs/{jobId}/pause")
    SchedulerJobDetail pauseJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId) throws WebApplicationException;

    @POST
    @Path("/{product}/{jobGroup}/jobs/{jobId}/resume")
    SchedulerJobDetail resumeJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId) throws WebApplicationException;


    @GET
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    SchedulerJobDetail getJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId) throws WebApplicationException;

    @PATCH
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    SchedulerJobDetail rescheduleJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId, JobRequest requestJson) throws WebApplicationException;

    @DELETE
    @Path("/{product}/{jobGroup}/jobs/{jobId}")
    void deleteJob(@PathParam("product") String product, @PathParam("jobGroup") String jobGroup, @PathParam("jobId") UUID jobId) throws WebApplicationException;
}
