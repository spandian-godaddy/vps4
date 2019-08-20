package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.security.TemporarilyDisabled;
import com.godaddy.vps4.web.snapshot.SnapshotActionResource;
import com.godaddy.vps4.web.snapshot.SnapshotActionWithDetails;
import com.google.inject.Inject;
import io.swagger.annotations.Api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.verifyUserPrivilegeToVm;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmSnapshotActionResource {
    private final GDUser user;
    private final Vps4UserService userService;
    private final SnapshotActionResource snapshotActionResource;
    private final PrivilegeService privilegeService;

    @Inject
    public VmSnapshotActionResource(GDUser user,
                                    Vps4UserService userService,
                                    SnapshotActionResource snapshotActionResource,
                                    PrivilegeService privilegeService) {
        this.user = user;
        this.userService = userService;
        this.snapshotActionResource = snapshotActionResource;
        this.privilegeService = privilegeService;
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/actions")
    public List<SnapshotAction> getActions(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        verifyUserPrivilege(vmId);
        return snapshotActionResource.getActions(snapshotId);
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/actions/{actionId}")
    public SnapshotAction getSnapshotAction(
            @PathParam("vmId") UUID vmId,
            @PathParam("snapshotId") UUID snapshotId,
            @PathParam("actionId") long actionId) {
        verifyUserPrivilege(vmId);
        return snapshotActionResource.getSnapshotAction(snapshotId, actionId);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/actions/{actionId}/withDetails")
    public SnapshotActionWithDetails getSnapshotActionWithDetails(
            @PathParam("vmId") UUID vmId,
            @PathParam("snapshotId") UUID snapshotId,
            @PathParam("actionId") long actionId) {
        verifyUserPrivilege(vmId);
        return snapshotActionResource.getSnapshotActionWithDetails(snapshotId, actionId);
    }

    private void verifyUserPrivilege(UUID vmId) {
        if (user.isShopper()) {
            verifyUserPrivilegeToVm(userService, privilegeService, user.getShopperId(), vmId);
        }
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @TemporarilyDisabled
    @POST
    @Path("/{vmId}/snapshots/{snapshotId}/actions/{actionId}/cancel")
    public void cancelSnapshotAction(
            @PathParam("vmId") UUID vmId,
            @PathParam("snapshotId") UUID snapshotId,
            @PathParam("actionId") long actionId) {
        verifyUserPrivilege(vmId);
        snapshotActionResource.cancelSnapshotAction(snapshotId, actionId);
    }
}
