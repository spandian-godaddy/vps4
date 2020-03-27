package com.godaddy.vps4.web.backupstorage;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsDedicated;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.backupstorage.BackupStorageCreds;
import com.godaddy.hfs.backupstorage.BackupStorage;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.backupstorage.BackupStorageService;
import com.godaddy.vps4.backupstorage.jdbc.BackupStorageModel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class BackupStorageResource {
    private static final Logger logger = LoggerFactory.getLogger(BackupStorageResource.class);
    private final GDUser user;
    private final ActionService actionService;
    private final BackupStorageService backupStorageService;
    private final CommandService commandService;
    private final CreditService creditService;
    private final VmResource vmResource;
    private final VmService vmService;

    @Inject
    public BackupStorageResource(GDUser user,
                                 ActionService actionService,
                                 BackupStorageService backupStorageService,
                                 CommandService commandService,
                                 CreditService creditService,
                                 VmResource vmResource,
                                 VmService vmService) {
        this.user = user;
        this.actionService = actionService;
        this.backupStorageService = backupStorageService;
        this.commandService = commandService;
        this.creditService = creditService;
        this.vmResource = vmResource;
        this.vmService = vmService;
    }

    @POST
    @Path("/{vmId}/ded/backupStorage")
    public VmAction createBackupStorage(@PathParam("vmId") UUID vmId) {
        logger.info("Creating backup storage for VM ID: {}", vmId);

        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        validateServerIsDedicated(vm, creditService);
        validateNoConflictingActions(vmId, actionService, ActionType.CREATE_BACKUP_STORAGE, ActionType.DESTROY_BACKUP_STORAGE);

        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.CREATE_BACKUP_STORAGE,
                                      request, "Vps4CreateBackupStorage", user);
    }

    @DELETE
    @Path("/{vmId}/ded/backupStorage")
    public VmAction destroyBackupStorage(@PathParam("vmId") UUID vmId) {
        logger.info("Destroying backup storage for VM ID: {}", vmId);

        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        validateServerIsDedicated(vm, creditService);
        validateNoConflictingActions(vmId, actionService, ActionType.CREATE_BACKUP_STORAGE, ActionType.DESTROY_BACKUP_STORAGE);

        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.DESTROY_BACKUP_STORAGE,
                                      request, "Vps4DestroyBackupStorage", user);
    }

    @GET
    @Path("/{vmId}/ded/backupStorage")
    public BackupStorage getBackupStorage(@PathParam("vmId") UUID vmId) {
        logger.info("Getting backup storage for VM ID: {}", vmId);

        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        validateServerIsDedicated(vm, creditService);

        try {
            return vmService.getBackupStorage(vm.hfsVmId);
        } catch (NotFoundException e) {
            logger.warn("Cannot find backup storage for VM ID {}", vmId);
            throw new NotFoundException("Backup storage not found");
        }
    }

    @POST
    @Path("/{vmId}/ded/backupStorage/credentials")
    public VmAction resetBackupStorageCreds(@PathParam("vmId") UUID vmId) {
        logger.info("Creating backup storage for VM ID: {}", vmId);

        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        validateServerIsDedicated(vm, creditService);
        validateNoConflictingActions(vmId, actionService, ActionType.RESET_BACKUP_STORAGE_CREDS);

        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.RESET_BACKUP_STORAGE_CREDS,
                                      request, "Vps4ResetBackupStorageCreds", user);
    }

    @GET
    @Path("/{vmId}/ded/backupStorage/credentials")
    public BackupStorageCreds getBackupStorageCreds(@PathParam("vmId") UUID vmId) {
        logger.info("Checking HFS for backup storage credentials matching VM ID: {}", vmId);

        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        validateServerIsDedicated(vm, creditService);

        try {
            return vmService.getBackupStorageCreds(vm.hfsVmId);
        } catch (NotFoundException ignored) {}

        logger.info("Checking database for backup storage credentials matching VM ID: {}", vmId);

        BackupStorageModel backup = backupStorageService.getBackupStorage(vmId);
        if (backup != null) {
            BackupStorageCreds creds = new BackupStorageCreds();
            creds.ftpServer = backup.ftpServer;
            creds.ftpUser = backup.ftpUser;
            return creds;
        }

        logger.warn("Cannot find backup storage creds for VM ID: {}", vmId);
        throw new NotFoundException("Backup storage creds not found");
    }
}
