package com.godaddy.vps4.web.client;

import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.ohbackup.OhBackupResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmOhBackupService {
    @GET
    @Path("/{vmId}/ohBackups")
    VmAction createOhBackup(@PathParam("vmId") UUID vmId, OhBackupResource.OhBackupRequest options);
}
