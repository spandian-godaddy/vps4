package com.godaddy.vps4.web.client;

import com.godaddy.vps4.vm.VmAction;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;


@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmService {
    @DELETE
    @Path("/{vmId}")
    VmAction destroyVm(@PathParam("vmId") UUID vmId);
}
