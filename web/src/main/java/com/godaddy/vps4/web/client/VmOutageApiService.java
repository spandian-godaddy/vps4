package com.godaddy.vps4.web.client;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.monitoring.VmOutageResource.VmOutageRequest;

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmOutageApiService {

    @POST
    @Path("{vmId}/outages")
    VmOutage newVmOutage(@PathParam("vmId") UUID vmId, VmOutageRequest req);

    @POST
    @Path("{vmId}/outages/{outageId}/clear")
    VmOutage clearVmOutage(@PathParam("vmId") UUID vmId,
            @PathParam("outageId") int outageId,
            @QueryParam("endDate") String endDate);
}
