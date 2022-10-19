package com.godaddy.vps4.web.messaging;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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
import com.godaddy.vps4.orchestration.messaging.FailOverEmailRequest;
import com.godaddy.vps4.orchestration.messaging.ScheduledMaintenanceEmailRequest;
import com.godaddy.vps4.orchestration.messaging.SetupCompletedEmailRequest;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.util.RequestValidation;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class VmMessagingResource {
    private static final Logger logger = LoggerFactory.getLogger(VmMessagingResource.class);

    private final VirtualMachineService virtualMachineService;
    private final CommandService commandService;
    private final CreditService creditService;
    private final GDUser user;

    @Inject
    public VmMessagingResource(VirtualMachineService virtualMachineService, CommandService commandService,
            CreditService creditService, GDUser user) {
        this.virtualMachineService = virtualMachineService;
        this.commandService = commandService;
        this.creditService = creditService;
        this.user = user;
    }

    @POST
    @Path("/{vmId}/messaging/patching")
    public MessagingResponse messagePatching(@PathParam("vmId") UUID vmId,
                                             @ApiParam(value = "startTime in GMT, Example: 2007-12-03T10:15:30.00Z. duration is in minutes.", required = true) ScheduledMessagingResourceRequest messageRequest)  {

        ScheduledMaintenanceEmailRequest request = createScheduledMaintenanceEmailRequest(vmId,
                messageRequest.startTime, messageRequest.durationMinutes);
        CommandState command = Commands.execute(commandService, "SendScheduledPatchingEmail", request);
        return new MessagingResponse(command.commandId, vmId, "SendScheduledPatchingEmail");
    }

	private ScheduledMaintenanceEmailRequest createScheduledMaintenanceEmailRequest(UUID vmId, String startTime, long durationMinutes) {
		Instant startTimeInstant = validateStartTime(startTime);
        validateDuration(durationMinutes);

        VirtualMachine vm = getAndValidateVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return new ScheduledMaintenanceEmailRequest(credit.getCustomerId(), vm.name, credit.isManaged(), startTimeInstant, durationMinutes);
	}

	private VirtualMachine getAndValidateVm(UUID vmId) {
		VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        RequestValidation.validateVmExists(vmId, vm, user);
        return vm;
	}

	private void validateDuration(long durationMinutes) {
        if(durationMinutes < 0) {
            throw new Vps4Exception("DURATION_NOT_VALID", "Provided duration is not valid");
        }
	}

	private Instant validateStartTime(String startTime) {
		Instant startTimeInstant;
		try {
            startTimeInstant = Instant.parse(startTime);
        } catch (DateTimeParseException e) {
            throw new Vps4Exception("START_TIME_NOT_VALID", "The provided start time is not a valid java Instant");
        }
        return startTimeInstant;
    }

    @POST
    @Path("/{vmId}/messaging/scheduledMaintenance")
    public MessagingResponse messageScheduledMaintenance(@PathParam("vmId") UUID vmId,
                                                         @ApiParam(value = "startTime in GMT, Example: 2007-12-03T10:15:30.00Z. duration is in minutes.", required = true) ScheduledMessagingResourceRequest messageRequest) {

        ScheduledMaintenanceEmailRequest request = createScheduledMaintenanceEmailRequest(vmId,
                messageRequest.startTime, messageRequest.durationMinutes);
        CommandState command = Commands.execute(commandService, "SendUnexpectedButScheduledMaintenanceEmail", request);
        return new MessagingResponse(command.commandId, vmId, "SendUnexpectedButScheduledMaintenanceEmail");
    }

    @POST
    @Path("/{vmId}/messaging/failover")
    public MessagingResponse messageFailover(@PathParam("vmId") UUID vmId) {
        FailOverEmailRequest request = createEmailRequest(vmId);
        CommandState command = Commands.execute(commandService, "SendSystemDownFailoverEmail", request);
        return new MessagingResponse(command.commandId, vmId, "SendSystemDownFailoverEmail");
    }

    private FailOverEmailRequest createEmailRequest(UUID vmId) {
		VirtualMachine vm = getAndValidateVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return new FailOverEmailRequest(credit.getCustomerId(), vm.name, credit.isManaged());
	}

    @POST
    @Path("/{vmId}/messaging/failoverComplete")
    public MessagingResponse messageFailoverComplete(@PathParam("vmId") UUID vmId) {
        FailOverEmailRequest request = createEmailRequest(vmId);
        CommandState command = Commands.execute(commandService, "SendFailoverCompletedEmail", request);
        return new MessagingResponse(command.commandId, vmId, "SendFailoverCompletedEmail");
    }


    @POST
    @Path("/{vmId}/messaging/setupCompleted")
    public MessagingResponse messageSetupCompleted(@PathParam("vmId") UUID vmId) {
        SetupCompletedEmailRequest request = createSetupCompleteEmailRequest(vmId);
        CommandState command = Commands.execute(commandService, "SendSetupCompletedEmail", request);
        return new MessagingResponse(command.commandId, vmId, "SendSetupCompletedEmail");
    }

    private SetupCompletedEmailRequest createSetupCompleteEmailRequest(UUID vmId) {
        VirtualMachine vm = getAndValidateVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return new SetupCompletedEmailRequest(credit.getCustomerId(), credit.isManaged(), vm.orionGuid, vm.name, vm.primaryIpAddress.ipAddress);
    }

    public class MessagingResponse {
        public UUID commandId;
        public UUID vmId;
        public String commandName;
        public MessagingResponse(UUID commandId, UUID vmId, String commandName){
            this.commandId = commandId;
            this.vmId = vmId;
            this.commandName = commandName;
        }
    }
}
