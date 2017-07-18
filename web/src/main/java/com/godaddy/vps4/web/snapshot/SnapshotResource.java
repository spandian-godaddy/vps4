package com.godaddy.vps4.web.snapshot;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotWithDetails;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Vps4Api
@Api(tags = {"snapshots"})

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class SnapshotResource {
    private final GDUser user;
    private final Vps4UserService userService;
    private final SnapshotService snapshotService;
    private final PrivilegeService privilegeService;

    @Inject
    public SnapshotResource(GDUser user,
                            Vps4UserService userService,
                            SnapshotService snapshotService,
                            PrivilegeService privilegeService) {
        this.user = user;
        this.userService = userService;
        this.snapshotService = snapshotService;
        this.privilegeService = privilegeService;
    }

    @GET
    @Path("/")
    public List<Snapshot> getSnapshotsForUser() {
        if (user.getShopperId() == null)
            throw new Vps4NoShopperException();
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());

        List<Snapshot> snapshots = snapshotService.getSnapshotsForUser(vps4User.getId());
        return snapshots.stream().filter(snapshot -> snapshot.status != SnapshotStatus.DESTROYED).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Snapshot getSnapshot(@PathParam("id") UUID id) {
        Snapshot snapshot = snapshotService.getSnapshot(id);

        if (snapshot == null || snapshot.status == SnapshotStatus.DESTROYED)
            throw new NotFoundException("Unknown snapshot ID: " + id);

        if (user.isShopper())
            verifyUserPrivilege(snapshot);

        return snapshot;
    }

    @AdminOnly
    @GET
    @Path("/{id}/withDetails")
    public SnapshotWithDetails getSnapshotWithDetails(@PathParam("id") UUID id) {
        SnapshotWithDetails snapshot = snapshotService.getSnapshotWithDetails(id);

        if (snapshot == null || snapshot.status == SnapshotStatus.DESTROYED)
            throw new NotFoundException("Unknown snapshot ID: " + id);

        if (user.isShopper())
            verifyUserPrivilege(snapshot);

        return snapshot;
    }

    private void verifyUserPrivilege(Snapshot snapshot) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());
        privilegeService.requireAnyPrivilegeToProjectId(vps4User, snapshot.projectId);
    }
}
