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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.orchestration.messaging.FailOverEmailRequest;
import com.godaddy.vps4.orchestration.messaging.ScheduledMaintenanceEmailRequest;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.AdminOnly;
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

public class VmMessagingResource {

    private final VirtualMachineService virtualMachineService;
    private final Vps4UserService vps4UserService;
    private final CommandService commandService;

    @Inject
    public VmMessagingResource(VirtualMachineService virtualMachineService, Vps4UserService vps4UserService, CommandService commandService) {
        this.virtualMachineService = virtualMachineService;
        this.vps4UserService = vps4UserService;
        this.commandService = commandService;
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/messaging/patching")
    public void messagePatching(@PathParam("vmId") UUID vmId,
            @ApiParam(value = "The start time of the patching window in GMT", required = true) @QueryParam("startTime") String startTime,
            @ApiParam(value = "The duration of the patching window in minutes", required = true) @QueryParam("duration") long durationMinutes) {
        ScheduledMaintenanceEmailRequest request = CreateMessagingRequest(vmId, startTime, durationMinutes,
                EmailTemplates.VPS4ScheduledPatchingV2);
        Commands.execute(commandService, "SendMessagingEmail", request);
    }

	private ScheduledMaintenanceEmailRequest CreateMessagingRequest(UUID vmId, String startTime, long durationMinutes, EmailTemplates template) {
		Instant startTimeInstant = validateStartTime(startTime);
        validateDuration(durationMinutes);

        VirtualMachine vm = getAndValidateVm(vmId);
        String shopperId = getShopperId(vm);

        return new ScheduledMaintenanceEmailRequest(template, shopperId, vm.name, vm.isFullyManaged(), startTimeInstant, durationMinutes);
	}

	private VirtualMachine getAndValidateVm(UUID vmId) {
		VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        RequestValidation.validateVmExists(vmId, vm);
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

    @AdminOnly
    @POST
    @Path("/{vmId}/messaging/scheduledMaintenance")
    public void messageScheduledMaintenance(@PathParam("vmId") UUID vmId,
            @ApiParam(value = "The start time of the maintenance window in GMT, example: 2007-12-03T10:15:30.00Z", required = true) @QueryParam("startTime") String startTime,
            @ApiParam(value = "The duration of the maintenance window in minutes", required = true) @QueryParam("duration") long durationMinutes) {
        ScheduledMaintenanceEmailRequest request = CreateMessagingRequest(vmId, startTime, durationMinutes,
                EmailTemplates.VPS4UnexpectedbutScheduledMaintenanceV2);
        Commands.execute(commandService, "SendMessagingEmail", request);
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/messaging/failover")
    public void messageFailover(@PathParam("vmId") UUID vmId) {
        FailOverEmailRequest request = CreateEmailRequest(vmId, EmailTemplates.VPS4SystemDownFailoverV2);
        Commands.execute(commandService, "SendMessagingEmail", request);
    }

    private FailOverEmailRequest CreateEmailRequest(UUID vmId, EmailTemplates template) {
		VirtualMachine vm = getAndValidateVm(vmId);
        String shopperId = getShopperId(vm);

        return new FailOverEmailRequest(template, shopperId, vm.name, vm.isFullyManaged());
	}

	private String getShopperId(VirtualMachine vm) {
		long vps4UserId = virtualMachineService.getUserIdByVmId(vm.vmId);
        String shopperId = vps4UserService.getUser(vps4UserId).getShopperId();
		return shopperId;
	}

	@AdminOnly
    @POST
    @Path("/{vmId}/messaging/failoverComplete")
    public void messageFailoverComplete(@PathParam("vmId") UUID vmId) {
        FailOverEmailRequest request = CreateEmailRequest(vmId, EmailTemplates.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted);
        Commands.execute(commandService, "SendMessagingEmail", request);
    }
}
