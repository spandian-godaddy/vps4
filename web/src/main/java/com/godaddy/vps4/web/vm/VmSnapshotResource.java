package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.verifyUserPrivilegeToVm;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonView;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmSnapshotResource {
    private final GDUser user;
    private final Vps4UserService userService;
    private final SnapshotService snapshotService;
    private final SnapshotResource snapshotResource;
    private final PrivilegeService privilegeService;

    @Inject
    public VmSnapshotResource(GDUser user,
                              Vps4UserService userService,
                              SnapshotResource snapshotResource,
                              SnapshotService snapshotService,
                              PrivilegeService privilegeService) {
        this.user = user;
        this.userService = userService;
        this.snapshotResource = snapshotResource;
        this.snapshotService = snapshotService;
        this.privilegeService = privilegeService;
    }

    @GET
    @Path("/{vmId}/snapshots")
    @JsonView(Views.Public.class)
    public List<Snapshot> getSnapshotsForVM(@PathParam("vmId") UUID vmId) {
        verifyUserPrivilege(vmId);
        return snapshotService.getSnapshotsForVm(vmId);
    }

    public static class VmSnapshotRequest {
        public String name;
    }

    @POST
    @Path("/{vmId}/snapshots")
    public SnapshotAction createSnapshot(@PathParam("vmId") UUID vmId, VmSnapshotRequest vmSnapshotRequest) {
        verifyUserPrivilege(vmId);

        SnapshotResource.SnapshotRequest request = new SnapshotResource.SnapshotRequest();
        request.vmId = vmId;
        request.name = vmSnapshotRequest.name;
        return snapshotResource.createSnapshot(request);
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}")
    @JsonView(Views.Public.class)
    public Snapshot getSnapshot(
            @PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        verifyUserPrivilege(vmId);
        return snapshotResource.getSnapshot(snapshotId);
    }

    @AdminOnly
    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/withDetails")
    @JsonView(Views.Internal.class)
    public Snapshot getSnapshotWithDetails(
            @PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        verifyUserPrivilege(vmId);
        return snapshotResource.getSnapshotWithDetails(snapshotId);
    }

    private void verifyUserPrivilege(UUID vmId) {
        if (user.isShopper()) {
            verifyUserPrivilegeToVm(userService, privilegeService, user.getShopperId(), vmId);
        }
    }
}
