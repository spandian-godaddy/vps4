package com.godaddy.vps4.web.monitoring;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import com.godaddy.vps4.orchestration.monitoring.VmOutageEmailRequest;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmOutage;
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

    private static final Logger logger = LoggerFactory.getLogger(VmOutageResource.class);

    private final VmResource vmResource;
    private final CommandService commandService;
    private final CreditService creditService;
    private final PanoptaService panoptaService;

    @Inject
    public VmOutageResource(VmResource vmResource,
                            CommandService commandService,
                            CreditService creditService,
                            PanoptaService panoptaService) {
        this.vmResource = vmResource;
        this.commandService = commandService;
        this.creditService = creditService;
        this.panoptaService = panoptaService;
    }

    @GET
    @Path("/{vmId}/outages")
    public List<VmOutage> getVmOutageList(@PathParam("vmId") UUID vmId,
                                          @QueryParam("activeOnly") @DefaultValue("true") boolean activeOnly)
            throws PanoptaServiceException {
        vmResource.getVm(vmId); // Auth validation
        return panoptaService.getOutages(vmId, activeOnly);
    }

    @GET
    @Path("/{vmId}/outages/{outageId}")
    public VmOutage getVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") int outageId)
            throws PanoptaServiceException {
        vmResource.getVm(vmId);  // Auth validation
        return panoptaService.getOutage(vmId, outageId);
    }

    @POST
    @RequiresRole(roles = {GDUser.Role.ADMIN}) // From message consumer
    @Path("/{vmId}/outages/{outageId}")
    public VmOutage newVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") long outageId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId); // Auth validation
        VmOutage outage;
        try {
            outage = panoptaService.getOutage(vmId, outageId);
        } catch (PanoptaServiceException e) {
            throw new Vps4Exception(e.getId(), e.getMessage(), e);
        }

        logger.info("New outage {} reported for VM {}", outageId, vmId);
        sendOutageNotificationEmail(vmId, virtualMachine, "SendVmOutageEmail", outage);

        return outage;
    }

    @POST
    @RequiresRole(roles = {GDUser.Role.ADMIN}) // From message consumer
    @Path("/{vmId}/outages/{outageId}/clear")
    public VmOutage clearVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") long outageId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId); // Auth validation
        VmOutage outage;
        try {
            outage = panoptaService.getOutage(vmId, outageId);
        } catch (PanoptaServiceException e) {
            throw new Vps4Exception(e.getId(), e.getMessage(), e);
        }

        logger.info("Clearing outage {} for VM {}", outageId, vmId);
        sendOutageNotificationEmail(vmId, virtualMachine, "SendVmOutageResolvedEmail", outage);

        return outage;
    }

    private void sendOutageNotificationEmail(UUID vmId, VirtualMachine virtualMachine,
                                             String emailOrchestrationClassname, VmOutage vmOutage) {

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        if (credit != null && credit.isAccountActive() && virtualMachine.isActive() && !credit.isManaged()) {
            VmOutageEmailRequest vmOutageEmailRequest =
                    new VmOutageEmailRequest(virtualMachine.name, virtualMachine.primaryIpAddress.ipAddress,
                                             credit.getOrionGuid(), credit.getShopperId(), vmId, credit.isManaged(),
                                             vmOutage);
            Commands.execute(commandService, emailOrchestrationClassname, vmOutageEmailRequest);
        }
    }

}
