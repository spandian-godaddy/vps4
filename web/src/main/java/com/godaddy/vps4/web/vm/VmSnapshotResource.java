package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;
import io.swagger.annotations.Api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmSnapshotResource {
    private final GDUser user;
    private final Vps4UserService userService;
    private final SnapshotService snapshotService;
    private final PrivilegeService privilegeService;

    @Inject
    public VmSnapshotResource(GDUser user,
                              Vps4UserService userService,
                              SnapshotService snapshotService,
                              PrivilegeService privilegeService) {
        this.user = user;
        this.userService = userService;
        this.snapshotService = snapshotService;
        this.privilegeService = privilegeService;
    }

    @GET
    @Path("/{vmId}/snapshots")
    public List<Snapshot> getSnapshotsForVM(@PathParam("vmId") UUID vmId) {
        if (user.isShopper())
            verifyUserPrivilege(vmId);

        return snapshotService.getSnapshotsForVm(vmId);
    }

    private void verifyUserPrivilege(UUID vmId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());
        privilegeService.requireAnyPrivilegeToVmId(vps4User, vmId);
    }
}
