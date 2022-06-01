package com.godaddy.vps4.oh.jobs;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.oh.OhResponse;
import com.godaddy.vps4.oh.jobs.models.OhJob;

@Path("/v2.1/job")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OhApiJobService {
    @GET
    @Path("/")
    OhResponse<OhJob> getJob(@QueryParam("uuid") UUID jobId);
}
