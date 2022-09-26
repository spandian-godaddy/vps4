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

import com.godaddy.vps4.orchestration.monitoring.Vps4NewVmOutage;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

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
    private final ActionService actionService;
    private final GDUser user;

    @Inject
    public VmOutageResource(VmResource vmResource,
                            CommandService commandService,
                            CreditService creditService,
                            PanoptaService panoptaService,
                            ActionService actionService,
                            GDUser user) {
        this.vmResource = vmResource;
        this.commandService = commandService;
        this.creditService = creditService;
        this.panoptaService = panoptaService;
        this.actionService = actionService;
        this.user = user;
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
    public VmAction newVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") long outageId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId); // Auth validation

        Vps4NewVmOutage.Request request = new Vps4NewVmOutage.Request();
        request.virtualMachine = virtualMachine;
        request.outageId = outageId;
        return createActionAndExecute(actionService, commandService, vmId, ActionType.NEW_VM_OUTAGE,
                request, "Vps4NewVmOutage", user);
    }

    @POST
    @RequiresRole(roles = {GDUser.Role.ADMIN}) // From message consumer
    @Path("/{vmId}/outages/{outageId}/clear")
    public VmAction clearVmOutage(@PathParam("vmId") UUID vmId, @PathParam("outageId") long outageId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId); // Auth validation

        Vps4NewVmOutage.Request request = new Vps4NewVmOutage.Request();
        request.virtualMachine = virtualMachine;

        return createActionAndExecute(actionService, commandService, vmId, ActionType.CLEAR_VM_OUTAGE,
                request, "Vps4ClearVmOutage", user);
    }

}
