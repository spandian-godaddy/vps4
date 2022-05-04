package com.godaddy.vps4.oh;


import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.oh.models.OhBackups;

@Path("/v2.1/backup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OhApiBackupService {
    @GET
    @Path("/")
    OhBackups getBackups(@QueryParam("package_uuid") UUID packageId);
}
