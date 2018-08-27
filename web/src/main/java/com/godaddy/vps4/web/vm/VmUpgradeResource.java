package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.*;

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
    private final GDUser user;

    @Inject
    public VmUpgradeResource(GDUser user, VirtualMachineService virtualMachineService, CreditService creditService,
            ActionService actionService) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.actionService = actionService;
        this.user = user;
    }

    @POST
    @Path("{vmId}/upgrade")
    public VmAction upgradeVm(@PathParam("vmId") UUID vmId) {
        logger.info("upgrading vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        validateVmExists(vmId, virtualMachine, user);

        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.RESTORE_VM, ActionType.CREATE_SNAPSHOT, ActionType.UPGRADE_VM);

        VirtualMachineCredit credit;
        if (user.isShopper()) {
            credit = getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
            if(!credit.planChangePending) {
                throw new Vps4Exception("NO_PLAN_CHANGE_PENDING", "No plan change is pending for VM " + vmId);
            }
        }

        //Call the new orchestration command here to process the upgrade when the command exists
        VmAction vmAction = new VmAction();
        vmAction.type = ActionType.UPGRADE_VM;
        return new VmAction();
    }
}
