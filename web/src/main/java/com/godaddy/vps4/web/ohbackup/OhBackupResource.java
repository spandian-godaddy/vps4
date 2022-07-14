package com.godaddy.vps4.web.ohbackup;

import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotOverQuota;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoOtherSnapshotsInProgress;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class OhBackupResource {
    private final GDUser user;
    private final VmResource vmResource;
    private final Config config;
    private final ActionService actionService;
    private final CommandService commandService;
    private final OhBackupService ohBackupService;
    private final SnapshotService snapshotService;

    @Inject
    public OhBackupResource(GDUser user,
                            VmResource vmResource,
                            Config config,
                            ActionService actionService,
                            CommandService commandService,
                            OhBackupService ohBackupService,
                            SnapshotService snapshotService) {
        this.user = user;
        this.vmResource = vmResource;
        this.config = config;
        this.actionService = actionService;
        this.commandService = commandService;
        this.ohBackupService = ohBackupService;
        this.snapshotService = snapshotService;
    }

    @GET
    @Path("/{vmId}/ohBackups")
    public List<OhBackup> getOhBackups(@PathParam("vmId") UUID vmId) {
        vmResource.getVm(vmId); // auth validation
        List<OhBackup> backups = ohBackupService.getBackups(vmId, OhBackupState.PENDING,
                                                            OhBackupState.COMPLETE, OhBackupState.FAILED);
        return new ArrayList<>(backups);
    }

    @POST
    @Path("/{vmId}/ohBackups")
    public VmAction createOhBackup(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        validateUserIsShopper(user);
        validateIfSnapshotOverQuota(ohBackupService, snapshotService, vm, SnapshotType.ON_DEMAND);
        validateNoOtherSnapshotsInProgress(ohBackupService, snapshotService, vm);

//        VmActionRequest vmActionRequest = new VmActionRequest();
//        vmActionRequest.virtualMachine = vm;
//        return createActionAndExecute(actionService, commandService, vmId, ActionType.CREATE_OH_BACKUP, vmActionRequest,
//                                      "Vps4CreateOhBackup", user);
        return null;
    }

    @GET
    @Path("/{vmId}/ohBackups/{backupId}")
    public OhBackup getOhBackup(@PathParam("vmId") UUID vmId, @PathParam("backupId") UUID backupId) {
        vmResource.getVm(vmId); // validates user owns VM
        return ohBackupService.getBackup(vmId, backupId); // validates backup corresponds with VM
    }

    @DELETE
    @Path("/{vmId}/ohBackups/{backupId}")
    public VmAction destroyOhBackup(@PathParam("vmId") UUID vmId, @PathParam("backupId") UUID backupId) {
        VirtualMachine vm = vmResource.getVm(vmId); // validates user owns VM
        ohBackupService.getBackup(vmId, backupId); // validates backup corresponds with VM
//        Vps4DestroyOhBackup.Request request = new Vps4DestroyOhBackup.Request();
//        request.virtualMachine = vm;
//        request.backupId = backupId;
//        return createActionAndExecute(actionService, commandService, vmId, ActionType.DESTROY_OH_BACKUP, request,
//                                      "Vps4DestroyOhBackup", user);
        return null;
    }

    @POST
    @Path("/{vmId}/ohBackups/{backupId}/restore")
    public VmAction restoreOhBackup(@PathParam("vmId") UUID vmId, @PathParam("backupId") UUID backupId) {
        VirtualMachine vm = vmResource.getVm(vmId); // validates user owns VM
        ohBackupService.getBackup(vmId, backupId); // validates backup corresponds with VM
//        Vps4RestoreOhBackup.Request request = new Vps4RestoreOhBackup.Request();
//        request.virtualMachine = vm;
//        request.backupId = backupId;
//        return createActionAndExecute(actionService, commandService, vmId, ActionType.RESTORE_OH_BACKUP, request,
//                                      "Vps4RestoreOhBackup", user);
        return null;
    }
}
