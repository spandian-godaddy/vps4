package com.godaddy.vps4.web.monitoring;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.monitoring.VmOutageEmailRequest;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.vm.VmOutageService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmOutageResource {

    private final VmResource vmResource;
    private final VmOutageService vmOutageService;
    private final CommandService commandService;
    private final CreditService creditService;

    @Inject
    public VmOutageResource(VmResource vmResource, VmOutageService vmOutageService,
            CommandService commandService, CreditService creditService) {
        this.vmResource = vmResource;
        this.vmOutageService = vmOutageService;
        this.commandService = commandService;
        this.creditService = creditService;
    }

    @GET
    @Path("/{vmId}/outages")
    public List<VmOutage> getVmOutageList(@PathParam("vmId") UUID vmId, @QueryParam("metric") String metric) {
        vmResource.getVm(vmId);  // Auth validation
        if (metric == null) {
            return vmOutageService.getVmOutageList(vmId);
        }

        VmMetric vmMetric = validateAndReturnEnumValue(VmMetric.class, metric);
        return vmOutageService.getVmOutageList(vmId, vmMetric);
    }

    @GET
    @Path("/{vmId}/outages/{outageId}")
    public VmOutage getVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") int outageId) {
        vmResource.getVm(vmId);  // Auth validation
        return vmOutageService.getVmOutage(outageId);
    }

    public static class VmOutageRequest {
        public String metric;
        public String startDate;
        public String reason;
        public long panoptaOutageId;
    }

    @POST
    @RequiresRole(roles = {GDUser.Role.ADMIN}) // From message consumer
    @Path("/{vmId}/outages/")
    public VmOutage newVmOutage(@PathParam("vmId") UUID vmId, VmOutageRequest req) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);  // Auth validation
        VmMetric vmMetric = validateAndReturnEnumValue(VmMetric.class, req.metric);
        Instant start = validatePanoptaDateAndReturnInstant(req.startDate);
        int outageId = vmOutageService.newVmOutage(vmId, vmMetric, start, req.reason, req.panoptaOutageId);
        return getVmOutageAndSendEmail(vmId, outageId, virtualMachine, "SendVmOutageEmail");
    }

    @POST
    @RequiresRole(roles = {GDUser.Role.ADMIN}) // From message consumer
    @Path("/{vmId}/outages/{outageId}/clear")
    public VmOutage clearVmOutage(@PathParam("vmId") UUID vmId,
                                  @PathParam("outageId") int outageId,
                                  @QueryParam("endDate") String endDate,
                                  @QueryParam("suppressEmail") boolean suppressEmail) {

        VirtualMachine virtualMachine = vmResource.getVm(vmId);  // Auth validation
        Instant end = Instant.now();
        if (endDate != null) {
            end = validatePanoptaDateAndReturnInstant(endDate);
        }
        vmOutageService.clearVmOutage(outageId, end);
        if (suppressEmail) {
            return vmOutageService.getVmOutage(outageId);
        }
        return getVmOutageAndSendEmail(vmId, outageId, virtualMachine, "SendVmOutageResolvedEmail");
    }

    private VmOutage getVmOutageAndSendEmail(UUID vmId, int outageId, VirtualMachine virtualMachine, String emailOrchestrationClassname) {
        VmOutage vmOutage = vmOutageService.getVmOutage(outageId);
        sendOutageNotificationEmail(vmId, virtualMachine, emailOrchestrationClassname, vmOutage);
        return vmOutage;
    }

    private void sendOutageNotificationEmail(UUID vmId, VirtualMachine virtualMachine,
            String emailOrchestrationClassname, VmOutage vmOutage) {

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        if (credit != null && credit.isAccountActive() && virtualMachine.isActive()) {
            VmOutageEmailRequest vmOutageEmailRequest =
                    new VmOutageEmailRequest(virtualMachine.name, virtualMachine.primaryIpAddress.ipAddress,
                                             credit.getOrionGuid(), credit.getShopperId(), vmId, credit.isManaged(),
                                             vmOutage);
            Commands.execute(commandService, emailOrchestrationClassname, vmOutageEmailRequest);
        }
    }

    public static final DateTimeFormatter PANOPTA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private Instant validatePanoptaDateAndReturnInstant(String dateToValidate) {
        try {
            return Instant.from(PANOPTA_DATE_FORMAT.parse(dateToValidate));
        } catch (DateTimeParseException e) {
            throw new Vps4Exception("INVALID_PARAMETER", String.format(
                    "Date %s has invalid format, use format such as 2011-12-03 10:15:30 UTC", dateToValidate));
        }
    }

}
