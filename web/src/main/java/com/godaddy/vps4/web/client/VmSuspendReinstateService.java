package com.godaddy.vps4.web.client;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VmAction;

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmSuspendReinstateService {

    @POST
    @Path("{vmId}/abuseSuspend")
    VmAction abuseSuspendAccount(@PathParam("vmId") UUID vmId);

    @POST
    @Path("{vmId}/billingSuspend")
    VmAction billingSuspendAccount(@PathParam("vmId") UUID vmId);

    @POST
    @Path("{vmId}/reinstateAbuseSuspend")
    VmAction reinstateAbuseSuspendedAccount(@PathParam("vmId") UUID vmId);

    @POST
    @Path("{vmId}/reinstateBillingSuspend")
    VmAction reinstateBillingSuspendedAccount(@PathParam("vmId") UUID vmId);
}
