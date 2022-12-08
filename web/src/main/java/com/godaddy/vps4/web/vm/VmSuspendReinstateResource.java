package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4SubmitSuspendServer;
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

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSuspendReinstateResource {

    private final GDUser user;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public VmSuspendReinstateResource(GDUser user, VmResource vmResource, CreditService creditService,
                                      ActionService actionService, CommandService commandService,
                                      VirtualMachineService virtualMachineService) {
        this.user = user;
        this.creditService = creditService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.virtualMachineService = virtualMachineService;
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
                    "This is a pass through to the corresponding HFS method. Reinstates for FRAUD or POLICY will " +
                    "attempt to reinstate for both FRAUD and POLICY reasons.")
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
