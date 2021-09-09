package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4UpgradeOHVm;
import com.godaddy.vps4.orchestration.vm.Vps4UpgradeVm;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.VmHelper;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.*;

import gdg.hfs.orchestration.CommandService;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmUpgradeResource {

    private static final Logger logger = LoggerFactory.getLogger(VmUpgradeResource.class);

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final Cryptography cryptography;
    private final String autoBackupName;
    private final Config config;

    @Inject
    public VmUpgradeResource(GDUser user, VirtualMachineService virtualMachineService, CreditService creditService,
                             ActionService actionService, CommandService commandService, Cryptography cryptography,
                             Config config) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.cryptography = cryptography;
        autoBackupName = config.get("vps4.autobackup.backupName");
        this.config = config;
    }

    public static class UpgradeVmRequest {
        public String password;
    }

    @POST
    @Path("{vmId}/upgrade")
    public VmAction upgradeVm(@PathParam("vmId") UUID vmId, UpgradeVmRequest upgradeVmRequest) {
        logger.info("upgrading vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        validateVmExists(vmId, virtualMachine, user);

        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.RESTORE_VM, ActionType.CREATE_SNAPSHOT, ActionType.UPGRADE_VM);

        VirtualMachineCredit credit = null;
        if (user.isShopper()) {
            credit = getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
            if (!credit.isPlanChangePending()) {
                throw new Vps4Exception("NO_PLAN_CHANGE_PENDING", "No plan change is pending for VM " + vmId);
            }
        }

        if (virtualMachine.spec.serverType.platform == ServerType.Platform.OPENSTACK) {
            validatePassword(upgradeVmRequest.password, virtualMachine.image);
        }

        String upgradeClassName = virtualMachine.spec.serverType.platform.getUpgradeCommand();
        ActionRequest commandRequest = generateUpgradeVmOrchestrationRequest(virtualMachine, credit,
                                                                             upgradeVmRequest);
        return VmHelper.createActionAndExecute(actionService, commandService, vmId, ActionType.UPGRADE_VM,
                                               commandRequest, upgradeClassName, user);
    }

    private ActionRequest generateUpgradeVmOrchestrationRequest(VirtualMachine vm, VirtualMachineCredit credit, UpgradeVmRequest upgradeVmRequest) {
        if (vm.spec.serverType.platform == ServerType.Platform.OPENSTACK) {
            Vps4UpgradeVm.Request req = new Vps4UpgradeVm.Request();
            req.vmId = vm.vmId;
            req.shopperId = user.getShopperId();
            req.initiatedBy = user.getUsername();
            req.encryptedPassword = cryptography.encrypt(upgradeVmRequest.password);
            req.newTier = credit.getTier();
            req.autoBackupName = autoBackupName;
            req.zone = config.get(vm.dataCenter.dataCenterName + ".openstack.zone");
            req.privateLabelId = credit.getResellerId();
            return req;
        }
        else if (vm.spec.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING) {
            return new Vps4UpgradeOHVm.Request(vm, credit.getTier());
        }
        else throw new Vps4Exception("UPGRADE_NOT_SUPPORTTED_FOR_PLATFORM", String.format("Upgrade not supported for platform %s", vm.spec.serverType.platform));
    }
}
