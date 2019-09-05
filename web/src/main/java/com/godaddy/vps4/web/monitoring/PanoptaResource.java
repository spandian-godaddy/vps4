package com.godaddy.vps4.web.monitoring;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Vps4Api
@Api(tags = {"panopta"})

@Path("/api/panopta")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class PanoptaResource {

    private static final Logger logger = LoggerFactory.getLogger(PanoptaResource.class);

    private final PanoptaService panoptaService;
    private final GDUser user;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;
    private final VmResource vmResource;
    private Config config;

    @Inject
    public PanoptaResource(PanoptaService panoptaService, GDUser user,
                           CreditService creditService, VirtualMachineService virtualMachineService,
                           VmResource vmResource, Config config) {
        this.panoptaService = panoptaService;
        this.user = user;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
        this.vmResource = vmResource;
        this.config = config;
    }

    @POST
    @Path("/customers")
    @ApiOperation(value = "Create a panopta customer for vps4.",
            notes = "Create a panopta customer for vps4.")
    public PanoptaCustomer createCustomer(CreateCustomerRequest request) {

        if (request == null || StringUtils.isBlank(request.vmId)) {
            throw new Vps4Exception("MISSING_VMID", "Missing Vm id in request.");
        }

        VirtualMachine virtualMachine = vmResource.getVm(UUID.fromString(request.vmId));

        //  Only vps4 credits are allowed panopta installations at the moment.
        // check credit and lookup spec to ensure credit is not for a ded4 server
        verifyCreditIsForVirtualServer(creditService, virtualMachine.orionGuid);

        try {
            return panoptaService.createCustomer(UUID.fromString(request.vmId));
        } catch (PanoptaServiceException e) {
            logger.warn("Encountered exception while creating customer in panopta: ", e);
            throw new Vps4Exception(e.getId(), e.getMessage(), e);
        }
    }

    // TODO: Remove after panopta installations are made available for ded4 servers.
    private void verifyCreditIsForVirtualServer(CreditService creditService, UUID orionGuid) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        ServerSpec spec = virtualMachineService.getSpec(credit.getTier());
        if (!spec.isVirtualMachine()) {
            throw new UnsupportedOperationException("Only virtual server type is supported for this operation.");
        }
    }

    @DELETE
    @Path("/customers/{vmId}")
    @ApiOperation(value = "Delete a vps4 panopta customer.",
            notes = "Delete a vps4 panopta customer.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Could not delete customer, or customer does not exist.")
    })
    public void deleteCustomer(@PathParam("vmId") UUID vmId) {
        panoptaService.deleteCustomer(vmId);
    }

    public static class CreateCustomerRequest {
        public String vmId;
    }

    @GET
    @Path("/server/{vmId}")
    @ApiOperation(value = "Get the vps4 server instance from panopta", notes = "Get the vps4 server instance from panopta")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Could not locate server in panopta, or server does not exist.")
    })
    public PanoptaServer getServer(@PathParam("vmId") UUID vmId) {
        try {
            return panoptaService.getServer(config.get("panopta.api.partner.customer.key.prefix") + vmId);
        } catch (PanoptaServiceException e) {
            logger.warn("Encountered exception while attempting to get server from panopta for vmId: " + vmId, e);
            throw new Vps4Exception(e.getId(), e.getMessage(), e);
        }
    }
}
