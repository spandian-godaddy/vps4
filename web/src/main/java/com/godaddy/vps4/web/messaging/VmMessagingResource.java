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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.util.RequestValidation;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class VmMessagingResource {

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
    public void messagePatching(@PathParam("vmId") UUID vmId,
            @ApiParam(value = "startTime in GMT, Example: 2007-12-03T10:15:30.00Z. duration is in minutes.", required = true) ScheduledMessagingResourceRequest messageRequest) {

        ScheduledMaintenanceEmailRequest request = createScheduledMaintenanceEmailRequest(vmId,
                messageRequest.startTime, messageRequest.durationMinutes);
        Commands.execute(commandService, "SendScheduledPatchingEmail", request);
    }

	private ScheduledMaintenanceEmailRequest createScheduledMaintenanceEmailRequest(UUID vmId, String startTime, long durationMinutes) {
		Instant startTimeInstant = validateStartTime(startTime);
        validateDuration(durationMinutes);

        VirtualMachine vm = getAndValidateVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        String shopperId = credit.getShopperId();

        return new ScheduledMaintenanceEmailRequest(shopperId, vm.name, credit.isManaged(), startTimeInstant, durationMinutes);
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
    public void messageScheduledMaintenance(@PathParam("vmId") UUID vmId,
            @ApiParam(value = "startTime in GMT, Example: 2007-12-03T10:15:30.00Z. duration is in minutes.", required = true) ScheduledMessagingResourceRequest messageRequest) {

        ScheduledMaintenanceEmailRequest request = createScheduledMaintenanceEmailRequest(vmId,
                messageRequest.startTime, messageRequest.durationMinutes);
        Commands.execute(commandService, "SendUnexpectedButScheduledMaintenanceEmail", request);
    }

    @POST
    @Path("/{vmId}/messaging/failover")
    public void messageFailover(@PathParam("vmId") UUID vmId) {
        FailOverEmailRequest request = createEmailRequest(vmId);
        Commands.execute(commandService, "SendSystemDownFailoverEmail", request);
    }

    private FailOverEmailRequest createEmailRequest(UUID vmId) {
		VirtualMachine vm = getAndValidateVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        String shopperId = credit.getShopperId();

        return new FailOverEmailRequest(shopperId, vm.name, credit.isManaged());
	}

    @POST
    @Path("/{vmId}/messaging/failoverComplete")
    public void messageFailoverComplete(@PathParam("vmId") UUID vmId) {
        FailOverEmailRequest request = createEmailRequest(vmId);
        Commands.execute(commandService, "SendFailoverCompletedEmail", request);
    }
}
