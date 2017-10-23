package com.godaddy.vps4.web.client;

import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRenameRequest;
import com.godaddy.vps4.web.vm.VmSnapshotResource.VmSnapshotRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;


@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VmSnapshotService {
    @GET
    @Path("/{vmId}/snapshots")
    List<Snapshot> getSnapshotsForVM(@PathParam("vmId") UUID vmId);

    @POST
    @Path("/{vmId}/snapshots")
    SnapshotAction createSnapshot(@PathParam("vmId") UUID vmId, VmSnapshotRequest vmSnapshotRequest);

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}")
    Snapshot getSnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId);

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/withDetails")
    Snapshot getSnapshotWithDetails(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId);

    @DELETE
    @Path("/{vmId}/snapshots/{snapshotId}")
    SnapshotAction destroySnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId);

    @PATCH
    @Path("/{vmId}/snapshots/{snapshotId}")
    SnapshotAction renameSnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId,
            SnapshotRenameRequest request);
}
