package com.godaddy.vps4.web.client;

import java.util.UUID;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VmAction;

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmSupportUserService {
    @DELETE
    @Path("/{vmId}/supportUsers/{supportUsername}")
    VmAction removeSupportUsers(@PathParam("vmId") UUID vmId, @PathParam("supportUsername") String username)
            throws WebApplicationException;
}
