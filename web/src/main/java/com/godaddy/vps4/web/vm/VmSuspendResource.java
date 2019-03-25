package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSuspendResource {

    private final GDUser user;
    private final VmResource vmResource;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public VmSuspendResource(GDUser user, VmResource vmResource, CreditService creditService,
            ActionService actionService, CommandService commandService, VirtualMachineService virtualMachineService) {
        this.user = user;
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.virtualMachineService = virtualMachineService;
    }

    @POST
    @Path("{vmId}/abuseSuspend")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.LEGAL, GDUser.Role.HS_OPS, GDUser.Role.DCU})
    public VmAction abuseSuspendVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateCreditForAbuseSuspend(vm, credit);

        String commandName = vm.spec.isVirtualMachine() ? "Vps4AbuseSuspendVm" : "Vps4AbuseSuspendDedicated";
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId,
                ActionType.ABUSE_SUSPEND, request, commandName, user);
    }

    private void validateCreditForAbuseSuspend(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        validateCredit(virtualMachine, credit);

        if (!credit.isAccountActive()) {
            throw new Vps4Exception("ACCOUNT_NOT_ACTIVE", String.format("The virtual machine account for orion guid %s is not active", virtualMachine.orionGuid));
        }
    }

    @POST
    @Path("{vmId}/reinstate")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.LEGAL, GDUser.Role.HS_OPS, GDUser.Role.DCU})
    public VmAction reinstateVm(@PathParam("vmId") UUID vmId) {
        // Cannot use vmResource.getVm as it returns 400 response if credit is suspended
        // TODO: Needs refactor. The GET /vms/<vmid> call should work even if vm suspended, but probably many
        //       api endpoints depend on this validation protection in getVm, so requires careful change
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        validateCreditForReinstate(vm, credit);

        String commandName = vm.spec.isVirtualMachine() ? "Vps4ReinstateVm" : "Vps4ReinstateDedicated";
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId,
                ActionType.REINSTATE, request, commandName, user);
    }

    private void validateCreditForReinstate(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        validateCredit(virtualMachine, credit);

        if(!credit.isAccountSuspended()) {
            throw new Vps4Exception("ACCOUNT_NOT_SUSPENDED", String.format("The virtual machine %s with orion guid %s is not suspended",
                    virtualMachine.vmId, virtualMachine.orionGuid));
        }
    }

    private void validateCredit(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        if (credit == null) {
            throw new Vps4Exception("CREDIT_NOT_FOUND",
                    String.format("The virtual machine credit for orion guid %s was not found", virtualMachine.orionGuid));
        }
    }
}
