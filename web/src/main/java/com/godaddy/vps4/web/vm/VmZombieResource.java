package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.Vps4ReviveZombieVm;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmZombieResource {

    private static final Logger logger = LoggerFactory.getLogger(VmZombieResource.class);

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final CommandService commandService;
    private final GDUser user;

    @Inject
    public VmZombieResource(VirtualMachineService virtualMachineService,
            CreditService creditService,
            CommandService commandService,
            GDUser user) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.commandService = commandService;
        this.user = user;
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/revive")
    @ApiOperation(value = "Revive a zombie vm whose account has been canceled but the server has not yet been deleted",
        notes = "Revive a zombie vm whose account has been canceled but the server has not yet been deleted")
    public VirtualMachine reviveZombieVm(
            @ApiParam(value = "The ID of the server to revive", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "The ID of the new credit to which the VM will be linked",
                    required = true) @QueryParam("newCreditId") UUID newCreditId) {

        logger.info("getting vm with id {}", vmId);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit oldCredit = creditService.getVirtualMachineCredit(vm.orionGuid);
        validateAccountIsRemoved(vmId, oldCredit);

        VirtualMachineCredit newCredit = getAndValidateUserAccountCredit(creditService, newCreditId, oldCredit.shopperId);
        validateCreditIsNotInUse(newCredit);

        validateCreditsMatch(oldCredit, newCredit);

        logger.info("Revive zombie vm: {}", vmId);

        Vps4ReviveZombieVm.Request request = new Vps4ReviveZombieVm.Request();
        request.vmId = vmId;
        request.newCreditId = newCreditId;
        request.oldCreditId = oldCredit.orionGuid;
        Commands.execute(commandService, "Vps4ReviveZombieVm", request);

        return virtualMachineService.getVirtualMachine(vmId);
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/zombie")
    @ApiOperation(value = "Zombie (stop and schedule deletion for) a VM whose account has been canceled",
        notes = "Zombie (stop and schedule deletion for) a VM whose account has been canceled")
    public VirtualMachine zombieVm(
            @ApiParam(value = "The ID of the server to zombie", required = true) @PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        validateAccountIsRemoved(vmId, credit);

        logger.info("Zombie vm: {}", vmId);
        Commands.execute(commandService, "Vps4ProcessAccountCancellation", credit);

        return virtualMachineService.getVirtualMachine(vmId);
    }

    private void validateCreditsMatch(VirtualMachineCredit oldCredit, VirtualMachineCredit newCredit) {
        if(!oldCredit.controlPanel.equalsIgnoreCase(newCredit.controlPanel)) {
            throw new Vps4Exception("CONTROL_PANEL_MISMATCH", "Control panel of old and new credits do not match");
        }
        if(oldCredit.managedLevel != newCredit.managedLevel) {
            throw new Vps4Exception("MANAGED_LEVEL_MISMATCH", "Managed level of the old and new credits do not match");
        }
        if(oldCredit.monitoring != newCredit.monitoring) {
            throw new Vps4Exception("MONITORING_MISMATCH", "Monitoring of the old and new credits do not match");
        }
        if(!oldCredit.operatingSystem.equalsIgnoreCase(newCredit.operatingSystem)) {
            throw new Vps4Exception("OPERATING_SYSTEM_MISMATCH", "Operating system of the old and new credits do not match");
        }
        if(oldCredit.tier != newCredit.tier) {
            throw new Vps4Exception("TIER_MISMATCH", "Tier of the old and new credits do not match");
        }
    }

    private void validateAccountIsRemoved(UUID vmId, VirtualMachineCredit credit) {
        if(!credit.isAccountRemoved()) {
            throw new Vps4Exception("ACCOUNT_STATUS_NOT_REMOVED", String.format("Cannot revive or zombie %s, account %s status is not removed.", vmId, credit.orionGuid));
        }
    }
}
