package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.snapshot.SnapshotStatus.CANCELLED;
import static com.godaddy.vps4.snapshot.SnapshotStatus.DESTROYED;

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
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.backups.OhBackupMapper;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRenameRequest;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmSnapshotResource {
    private final VmResource vmResource;
    private final SnapshotResource snapshotResource;
    private final OhBackupService ohBackupService;
    private final SnapshotService snapshotService;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;
    private final Config config;

    @Inject
    public VmSnapshotResource(VmResource vmResource,
                              SnapshotResource snapshotResource,
                              OhBackupService ohBackupService,
                              SnapshotService snapshotService,
                              VirtualMachineService virtualMachineService,
                              VmService vmService,
                              Config config) {
        this.vmResource = vmResource;
        this.snapshotResource = snapshotResource;
        this.ohBackupService = ohBackupService;
        this.snapshotService = snapshotService;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
        this.config = config;
    }

    private boolean shouldUseOhBackups(VirtualMachine vm) {
        return vm.image.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING
                && Boolean.parseBoolean(config.get("oh.backups.enabled", "false"));
    }

    @GET
    @Path("/{vmId}/snapshots")
    @JsonView(Views.Public.class)
    public List<Snapshot> getSnapshotsForVM(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        if (shouldUseOhBackups(vm)) {
            long projectId = virtualMachineService.getVirtualMachine(vmId).projectId;
            return ohBackupService.getBackups(vmId)
                    .stream()
                    .map(ohBackup -> OhBackupMapper.toSnapshot(ohBackup, vmId, projectId))
                    .collect(Collectors.toList());
        }
        return snapshotService.getSnapshotsForVm(vmId)
                .stream()
                .filter(snapshot -> snapshot.status != DESTROYED && snapshot.status != CANCELLED)
                .collect(Collectors.toList());
    }

    public static class VmSnapshotRequest {
        public String name;
        public SnapshotType snapshotType = SnapshotType.ON_DEMAND;
    }

    @POST
    @Path("/{vmId}/snapshots")
    public SnapshotAction createSnapshot(@PathParam("vmId") UUID vmId, VmSnapshotRequest vmSnapshotRequest) {
        if (Boolean.parseBoolean(config.get("vps4.snapshot.currentlyPaused"))) {
            throw new Vps4Exception("JOB_PAUSED", "Currently pausing all snapshot jobs. Refusing to take snapshot.");
        }

        SnapshotResource.SnapshotRequest request = new SnapshotResource.SnapshotRequest();
        request.vmId = vmId;
        request.name = vmSnapshotRequest.name;
        request.snapshotType = vmSnapshotRequest.snapshotType;

        return snapshotResource.createSnapshot(request);
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}")
    @JsonView(Views.Public.class)
    public Snapshot getSnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
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

    @GET
    @Path("/{vmId}/canSnapshotNow")
    @ApiOperation(value = "Check for DC and hypervisor snapshot limits to decide whether a snapshot can be requested right now for a VM",
            notes = "Returns true if neither DC snapshot limit nor hypervisor snapshot limit are reached; false otherwise")
    public boolean canSnapshotNow(
            @PathParam("vmId") UUID vmId) {
        int currentDCLoad = snapshotService.totalSnapshotsInProgress();
        int maxDCAllowed = Integer.parseInt(config.get("vps4.autobackup.concurrentLimit", "50"));

        if(Boolean.parseBoolean(config.get("vps4.snapshot.currentlyPaused")))
            return false;
        if (currentDCLoad >= maxDCAllowed)
            return false;
        if(Boolean.parseBoolean(config.get("vps4.autobackup.checkHvConcurrentLimit"))) {
            VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
            VmExtendedInfo vmExtendedInfo = vmService.getVmExtendedInfo(virtualMachine.hfsVmId);
            if(vmExtendedInfo != null) {
                UUID conflictingVmId = snapshotService.getVmIdWithInProgressSnapshotOnHv(vmExtendedInfo.extended.hypervisorHostname);
                return conflictingVmId == null;
            }
        }
        return true;
    }
}
