package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4ReinstateServer;
import com.godaddy.vps4.orchestration.vm.Vps4SubmitSuspendServer;
import com.godaddy.vps4.orchestration.vm.Vps4SuspendServer;
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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSuspendReinstateResource {

    private final GDUser user;
    private final VmResource vmResource;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public VmSuspendReinstateResource(GDUser user, VmResource vmResource, CreditService creditService,
                                      ActionService actionService, CommandService commandService,
                                      VirtualMachineService virtualMachineService) {
        this.user = user;
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.virtualMachineService = virtualMachineService;
    }

    @POST
    @Path("{vmId}/abuseSuspend")
    @ApiOperation(value = "Abuse suspend a server and the credit associated with it.",
                  notes = "Abuse suspend a server and the credit associated with it.")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.SUSPEND_AUTH})
    public VmAction abuseSuspendAccount(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        getAndValidateCredit(vm);
        return suspendServer(vm, ActionType.ABUSE_SUSPEND);
    }

    @POST
    @Path("{vmId}/billingSuspend")
    @ApiOperation(value = "Billing suspend a server and the credit associated with it.",
                  notes = "Billing suspend a server and the credit associated with it.")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public VmAction billingSuspendAccount(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        getAndValidateCredit(vm);
        return suspendServer(vm, ActionType.BILLING_SUSPEND);
    }

    private VmAction suspendServer(VirtualMachine vm, ActionType actionType) {

        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        request.actionType = actionType;
        String commandName =
                vm.spec.isVirtualMachine() ? "Vps4SuspendServer" : "Vps4SuspendDedServer";
        return createActionAndExecute(actionService, commandService, vm.vmId,
                                      actionType, request, commandName, user);
    }

    @POST
    @Path("{vmId}/reinstateAbuseSuspend")
    @ApiOperation(value = "Re-instate an abuse-suspended server.",
                  notes = "Re-instate an abuse-suspended server.")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.SUSPEND_AUTH})
    public VmAction reinstateAbuseSuspendedAccount(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit credit = getAndValidateCredit(vm);
        validateCreditIsAbuseSuspended(vm, credit);
        resetAbuseSuspendedFlagOnCredit(vm, credit);

        // billing suspended accounts cannot be reinstated from an abuse-suspend reinstate call.
        validateCreditIsNotBillingSuspended(vm, credit);

        return reinstateServer(vm, ECommCreditService.ProductMetaField.ABUSE_SUSPENDED_FLAG);
    }

    @POST
    @Path("{vmId}/reinstateBillingSuspend")
    @ApiOperation(value = "Re-instate a billing-suspended server.",
                  notes = "Re-instate a billing-suspended server.")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public VmAction reinstateBillingSuspendedAccount(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit credit = getAndValidateCredit(vm);
        validateCreditIsBillingSuspended(vm, credit);
        resetBillingSuspendedFlagOnCredit(vm, credit);

        // abuse suspended accounts cannot be reinstated from a billing-suspend reinstate call.
        validateCreditIsNotAbuseSuspended(vm, credit);

        return reinstateServer(vm, ECommCreditService.ProductMetaField.BILLING_SUSPENDED_FLAG);
    }

    @POST
    @Path("{vmId}/processSuspendMessage")
    @ApiOperation(value = "Process a suspend message received by the message handler",
            notes = "Process a suspend message received by the message handler")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public VmAction processSuspendMessage(@PathParam("vmId") UUID vmId){
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.PROCESS_SUSPEND, request,
                "Vps4ProcessSuspendServer", user);
    }

    @POST
    @Path("{vmId}/processReinstateMessage")
    @ApiOperation(value = "Process a reinstate message received by the message handler",
                  notes = "Process a reinstate Message received by the message handler")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public VmAction processReinstateMessage(@PathParam("vmId") UUID vmId){
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.PROCESS_REINSTATE, request,
                "Vps4ProcessReinstateServer", user);
    }

    private VmAction reinstateServer(VirtualMachine vm, ECommCreditService.ProductMetaField resetFlag) {
        Vps4ReinstateServer.Request request = new Vps4ReinstateServer.Request();
        request.virtualMachine = vm;
        request.resetFlag = resetFlag;
        String commandName =
                vm.spec.isVirtualMachine() ? "Vps4ReinstateServer" : "Vps4ReinstateDedServer";
        return createActionAndExecute(actionService, commandService, vm.vmId,
                                      ActionType.REINSTATE, request, commandName, user);
    }

    @POST
    @Path("{vmId}/suspend")
    @ApiOperation(value = "Suspend a server. This is a pass through to the corresponding HFS method.",
            notes = "Suspend a server. " +
                    "This is a pass through to the corresponding HFS method.")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.SUSPEND_AUTH})
    public VmAction suspendVm(@PathParam("vmId") UUID vmId,
                              @ApiParam(value = "Available reasons are FRAUD, LEGAL, POLICY.", required = true) @QueryParam("reason") String reason) {
        Vps4SubmitSuspendServer.Request request = new Vps4SubmitSuspendServer.Request();
        request.reason = validateAndReturnEnumValue(ECommCreditService.SuspensionReason.class, reason);;

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);
        request.virtualMachine = vm;
        getAndValidateCredit(vm);

        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.SUBMIT_SUSPEND, request,
                "Vps4SubmitSuspendServer", user);
    }

    @POST
    @Path("{vmId}/reinstate")
    @ApiOperation(value = "Reinstate a server. This is a pass through to the corresponding HFS method.",
            notes = "Reinstate a server. " +
                    "This is a pass through to the corresponding HFS method.")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.SUSPEND_AUTH})
    public VmAction reinstateVm(@PathParam("vmId") UUID vmId,
                                @ApiParam(value = "Available reasons are FRAUD, LEGAL, POLICY.", required = true) @QueryParam("reason") String reason) {
        Vps4SubmitSuspendServer.Request request = new Vps4SubmitSuspendServer.Request();
        request.reason = validateAndReturnEnumValue(ECommCreditService.SuspensionReason.class, reason);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);
        request.virtualMachine = vm;
        getAndValidateCredit(vm);

        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.SUBMIT_REINSTATE, request,
                "Vps4SubmitReinstateServer", user);
    }

    private void validateCreditIsNotBillingSuspended(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        if(credit.isBillingSuspendedFlagSet()) {
            throw new Vps4Exception("ACCOUNT_IS_BILLING_SUSPENDED", String.format(
                    "The virtual machine %s with orion guid %s is billing suspended. A 'billing suspend' reinstate needs" +
                    " to be initiated.",
                    virtualMachine.vmId, virtualMachine.orionGuid));
        }
    }

    private void resetAbuseSuspendedFlagOnCredit(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        if (credit.isAbuseSuspendedFlagSet() && credit.isBillingSuspendedFlagSet()) {
            // Just update the product meta on the credit to reset the abuse suspended flag.
            creditService.setAbuseSuspendedFlag(virtualMachine.orionGuid, false);
        }
    }

    private void validateCreditIsAbuseSuspended(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        if (!credit.isAbuseSuspendedFlagSet() || !credit.isAccountSuspended()) {
            throw new Vps4Exception("ACCOUNT_IS_NOT_SUSPENDED",
                                    String.format("The virtual machine %s with orion guid %s is NOT suspended.",
                                                  virtualMachine.vmId, virtualMachine.orionGuid));
        }
    }

    private void validateCreditIsBillingSuspended(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        if (!credit.isBillingSuspendedFlagSet() && !credit.isAccountSuspended()) {
                throw new Vps4Exception("ACCOUNT_IS_NOT_SUSPENDED",
                                    String.format("The virtual machine %s with orion guid %s is NOT suspended.",
                                                  virtualMachine.vmId, virtualMachine.orionGuid));
        }
    }

    private void resetBillingSuspendedFlagOnCredit(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        if (credit.isAbuseSuspendedFlagSet() && credit.isBillingSuspendedFlagSet()) {
            // Just update the product meta on the credit to reset the billing suspended flag.
            creditService.setBillingSuspendedFlag(virtualMachine.orionGuid, false);
        }
    }

    private void validateCreditIsNotAbuseSuspended(VirtualMachine virtualMachine, VirtualMachineCredit credit) {
        // account cannot be re-instated if it has been abuse suspended.
        if(credit.isAbuseSuspendedFlagSet()) {
            throw new Vps4Exception("ACCOUNT_IS_ABUSE_SUSPENDED", String.format(
                    "The virtual machine %s with orion guid %s is abuse suspended. An 'abuse suspend' reinstate needs" +
                    " to be initiated.",
                    virtualMachine.vmId, virtualMachine.orionGuid));
        }
    }

    private VirtualMachineCredit getAndValidateCredit(VirtualMachine vm) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        if (credit == null) {
            throw new Vps4Exception("CREDIT_NOT_FOUND",
                                    String.format("The virtual machine credit for orion guid %s was not found",
                                                  vm.orionGuid));
        }
        return credit;
    }
}
