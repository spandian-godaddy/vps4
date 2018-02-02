package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonView;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRenameRequest;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmSnapshotResource {
    private final GDUser user;
    private final CreditService creditService;
    private final SnapshotService snapshotService;
    private final SnapshotResource snapshotResource;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public VmSnapshotResource(GDUser user,
                              CreditService creditService,
                              SnapshotResource snapshotResource,
                              SnapshotService snapshotService,
                              VirtualMachineService virtualMachineService
                              ) {
        this.user = user;
        this.creditService = creditService;
        this.snapshotResource = snapshotResource;
        this.snapshotService = snapshotService;
        this.virtualMachineService = virtualMachineService;
    }

    @GET
    @Path("/{vmId}/snapshots")
    @JsonView(Views.Public.class)
    public List<Snapshot> getSnapshotsForVM(@PathParam("vmId") UUID vmId) {
        // check to ensure snapshot belongs to vm and vm exists
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, virtualMachine);
        if (user.isShopper()) {
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
        }

        return snapshotService.getSnapshotsForVm(vmId)
                .stream()
                .filter(snapshot -> snapshot.status != SnapshotStatus.DESTROYED && snapshot.status != SnapshotStatus.CANCELLED)
                .collect(Collectors.toList());
    }

    public static class VmSnapshotRequest {
        public String name;
        public SnapshotType snapshotType = SnapshotType.ON_DEMAND;
    }

    @POST
    @Path("/{vmId}/snapshots")
    public SnapshotAction createSnapshot(@PathParam("vmId") UUID vmId, VmSnapshotRequest vmSnapshotRequest) {
        SnapshotResource.SnapshotRequest request = new SnapshotResource.SnapshotRequest();
        request.vmId = vmId;
        request.name = vmSnapshotRequest.name;
        request.snapshotType = vmSnapshotRequest.snapshotType;

        return snapshotResource.createSnapshot(request);
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}")
    @JsonView(Views.Public.class)
    public Snapshot getSnapshot(
            @PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        return snapshotResource.getSnapshot(snapshotId);
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/withDetails")
    @JsonView(Views.Internal.class)
    public Snapshot getSnapshotWithDetails(
            @PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        return getSnapshot(vmId, snapshotId);
    }

    @DELETE
    @Path("/{vmId}/snapshots/{snapshotId}")
    public SnapshotAction destroySnapshot(
            @PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        return snapshotResource.destroySnapshot(snapshotId);
    }

    @PATCH
    @Path("/{vmId}/snapshots/{snapshotId}")
    public SnapshotAction renameSnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId,
            SnapshotRenameRequest request) {
        return snapshotResource.renameSnapshot(snapshotId, request);
    }


}
