package com.godaddy.vps4.web.ohbackup;

import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotOverQuota;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoOtherSnapshotsInProgress;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerPlatform;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotName;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.NamedOhBackup;
import com.godaddy.vps4.oh.backups.OhBackupData;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupPurpose;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.orchestration.ohbackup.Vps4CreateOhBackup;
import com.godaddy.vps4.orchestration.ohbackup.Vps4DestroyOhBackup;
import com.godaddy.vps4.orchestration.ohbackup.Vps4RestoreOhBackup;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OhBackupResource {
    private static final Logger logger = LoggerFactory.getLogger(OhBackupResource.class);
    private final GDUser user;
    private final VmResource vmResource;
    private final Config config;
    private final ActionService actionService;
    private final CommandService commandService;
    private final OhBackupService ohBackupService;
    private final OhBackupDataService ohBackupDataService;
    private final SnapshotService snapshotService;

    @Inject
    public OhBackupResource(GDUser user,
                            VmResource vmResource,
                            Config config,
                            ActionService actionService,
                            CommandService commandService,
                            OhBackupService ohBackupService,
                            OhBackupDataService ohBackupDataService,
                            SnapshotService snapshotService) {
        this.user = user;
        this.vmResource = vmResource;
        this.config = config;
        this.actionService = actionService;
        this.commandService = commandService;
        this.ohBackupService = ohBackupService;
        this.ohBackupDataService = ohBackupDataService;
        this.snapshotService = snapshotService;
    }

    private void validateOhBackupsAreEnabled() {
        boolean areBackupsEnabled = Boolean.parseBoolean(config.get("oh.backups.enabled", "false"));
        if (!areBackupsEnabled) {
            throw new Vps4Exception("FEATURE_DISABLED", "The OH backups feature is currently disabled.");
        }
    }

    private void validateOhBackupState(OhBackup backup, OhBackupState state) {
        if (backup.state != state) {
            throw new Vps4Exception("INVALID_STATE", "OH backup must be '" + state + "' for this operation");
        }
    }

    @GET
    @Path("/{vmId}/ohBackups")
    public List<NamedOhBackup> getOhBackups(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        validateServerPlatform(vm, ServerType.Platform.OPTIMIZED_HOSTING);
        List<OhBackup> backups = ohBackupService.getBackups(vmId, OhBackupState.PENDING,
                                                            OhBackupState.COMPLETE, OhBackupState.FAILED);
        List<OhBackupData> ohBackupData = ohBackupDataService.getBackups(vm.vmId);

        List<OhBackup> automatic = backups.stream()
                                          .filter(b -> b.purpose == OhBackupPurpose.DR)
                                          .collect(Collectors.toList());
        List<OhBackup> onDemand = backups.stream()
                                         .filter(b -> b.purpose == OhBackupPurpose.CUSTOMER)
                                         .collect(Collectors.toList());

        // remove any customer OH backups that aren't tracked in our DB, since they are actually HFS snapshots
        onDemand.removeIf(b -> ohBackupData.stream().noneMatch(obd -> obd.backupId.equals(b.id)));

        // remove automatic backups that are pending/failed, unless they are the most recent automatic backup
        Optional<OhBackup> lastAutomatic = automatic.stream()
                                                    .reduce((b1, b2) -> b1.createdAt.isAfter(b2.createdAt) ? b1 : b2);
        automatic.removeIf(b -> b.state != OhBackupState.COMPLETE && !lastAutomatic.get().id.equals(b.id));

        // only return the last 7 automatic backups
        automatic.sort((b1, b2) -> b2.createdAt.compareTo(b1.createdAt));
        automatic = automatic.subList(0, Math.min(7, automatic.size()));

        // use the backup names from our database if available
        String defaultName = config.get("vps4.autobackup.backupName");
        return Stream.concat(onDemand.stream(), automatic.stream()).map(b -> {
            Optional<OhBackupData> data = ohBackupData.stream()
                                                      .filter(obd -> obd.backupId.equals(b.id))
                                                      .findFirst();
            return data.map(backupData -> new NamedOhBackup(b, backupData.name))
                       .orElseGet(() -> new NamedOhBackup(b, defaultName));
        }).collect(Collectors.toList());
    }

    @POST
    @Path("/{vmId}/ohBackups")
    public VmAction createOhBackup(@PathParam("vmId") UUID vmId, OhBackupRequest options) {
        logger.debug("Begin OH Backup request validation for vm {}", vmId);
        validateOhBackupsAreEnabled();
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        validateSnapshotName(options.name);
        validateServerPlatform(vm, ServerType.Platform.OPTIMIZED_HOSTING);
        validateNoConflictingActions(vm.vmId, actionService, ActionType.CREATE_OH_BACKUP,
                                     ActionType.DESTROY_OH_BACKUP, ActionType.RESTORE_OH_BACKUP);
        validateIfSnapshotOverQuota(ohBackupDataService, snapshotService, vm, SnapshotType.ON_DEMAND);
        validateNoOtherSnapshotsInProgress(ohBackupService, snapshotService, vm);

        logger.debug("Finished OH backup request validation for vm {}, call OH to create backup");
        Vps4CreateOhBackup.Request request = new Vps4CreateOhBackup.Request(vm, user.getUsername(), options.name);
        return createActionAndExecute(actionService, commandService, vmId, ActionType.CREATE_OH_BACKUP, request,
                                      "Vps4CreateOhBackup", user);
    }

    public static class OhBackupRequest {
        public String name;

        public OhBackupRequest() {} // needed for deserialization

        public OhBackupRequest(String name) {
            this.name = name;
        }
    }

    @GET
    @Path("/{vmId}/ohBackups/{backupId}")
    public NamedOhBackup getOhBackup(@PathParam("vmId") UUID vmId, @PathParam("backupId") UUID backupId) {
        VirtualMachine vm = vmResource.getVm(vmId); // validates user owns VM
        validateServerPlatform(vm, ServerType.Platform.OPTIMIZED_HOSTING);

        OhBackup backup = ohBackupService.getBackup(vmId, backupId); // validates backup corresponds with VM
        OhBackupData ohBackupData = ohBackupDataService.getBackup(backupId);

        String defaultName = config.get("vps4.autobackup.backupName");
        return (ohBackupData == null)
                ? new NamedOhBackup(backup, defaultName)
                : new NamedOhBackup(backup, ohBackupData.name);
    }

    @DELETE
    @Path("/{vmId}/ohBackups/{backupId}")
    public VmAction destroyOhBackup(@PathParam("vmId") UUID vmId, @PathParam("backupId") UUID backupId) {
        validateOhBackupsAreEnabled();
        VirtualMachine vm = vmResource.getVm(vmId); // validates user owns VM
        validateServerPlatform(vm, ServerType.Platform.OPTIMIZED_HOSTING);
        validateNoConflictingActions(vm.vmId, actionService, ActionType.CREATE_OH_BACKUP,
                                     ActionType.DESTROY_OH_BACKUP, ActionType.RESTORE_OH_BACKUP);
        ohBackupService.getBackup(vmId, backupId); // validates backup corresponds with VM

        Vps4DestroyOhBackup.Request request = new Vps4DestroyOhBackup.Request(vm, backupId);
        return createActionAndExecute(actionService, commandService, vmId, ActionType.DESTROY_OH_BACKUP, request,
                                      "Vps4DestroyOhBackup", user);
    }

    @POST
    @Path("/{vmId}/ohBackups/{backupId}/restore")
    public VmAction restoreOhBackup(@PathParam("vmId") UUID vmId, @PathParam("backupId") UUID backupId) {
        validateOhBackupsAreEnabled();
        VirtualMachine vm = vmResource.getVm(vmId); // validates user owns VM
        validateServerPlatform(vm, ServerType.Platform.OPTIMIZED_HOSTING);
        validateNoConflictingActions(vm.vmId, actionService, ActionType.CREATE_OH_BACKUP,
                                     ActionType.DESTROY_OH_BACKUP, ActionType.RESTORE_OH_BACKUP);
        OhBackup backup = ohBackupService.getBackup(vmId, backupId); // validates backup corresponds with VM
        validateOhBackupState(backup, OhBackupState.COMPLETE);

        Vps4RestoreOhBackup.Request request = new Vps4RestoreOhBackup.Request(vm, backupId);
        return createActionAndExecute(actionService, commandService, vmId, ActionType.RESTORE_OH_BACKUP, request,
                                      "Vps4RestoreOhBackup", user);
    }
}
