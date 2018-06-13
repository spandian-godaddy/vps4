package com.godaddy.vps4.web.client;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VmAction;

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmSupportUserService {
    @DELETE
    @Path("/{vmId}/supportUser")
    VmAction removeSupportUser(@PathParam("vmId") UUID vmId);
}
