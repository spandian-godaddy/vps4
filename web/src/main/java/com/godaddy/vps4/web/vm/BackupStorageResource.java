package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsDedicated;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;

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
    private final CommandService commandService;
    private final CreditService creditService;
    private final VmResource vmResource;

    @Inject
    public BackupStorageResource(GDUser user,
                               ActionService actionService,
                               CommandService commandService,
                               CreditService creditService,
                               VmResource vmResource) {
        this.user = user;
        this.actionService = actionService;
        this.commandService = commandService;
        this.creditService = creditService;
        this.vmResource = vmResource;
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
}
