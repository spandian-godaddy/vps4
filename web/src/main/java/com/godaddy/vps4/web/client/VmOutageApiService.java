package com.godaddy.vps4.web.client;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmOutage;

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmOutageApiService {

    @POST
    @Path("{vmId}/outages/{outageId}")
    VmAction newVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") long outageId);

    @POST
    @Path("{vmId}/outages/{outageId}/clear")
    VmAction clearVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") long outageId);
}
