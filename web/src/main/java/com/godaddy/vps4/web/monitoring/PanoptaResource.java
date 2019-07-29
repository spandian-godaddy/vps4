package com.godaddy.vps4.web.monitoring;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import com.godaddy.vps4.panopta.PanoptaCustomerRequest;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
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
public class PanoptaResource {

    private static final Logger logger = LoggerFactory.getLogger(PanoptaResource.class);

    private final PanoptaService panoptaService;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;
    private final GDUser user;
    private Config config;

    @Inject
    public PanoptaResource(PanoptaService panoptaService, CreditService creditService,
                           VirtualMachineService virtualMachineService, GDUser user, Config config) {
        this.panoptaService = panoptaService;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
        this.user = user;
        this.config = config;
    }

    @POST
    @Path("/customers")
    @ApiOperation(value = "Create a panopta customer for vps4.",
            notes = "Create a panopta customer for vps4.")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public PanoptaCustomer createCustomer(CreateCustomerRequest request) {

        if(request == null) {
            throw new Vps4Exception("MISSING_ORIONGUID", "Missing Orion GUID in request.");
        }

        if(StringUtils.isBlank(request.orionGuid)) {
            throw new Vps4Exception("MISSING_ORIONGUID", "Missing Orion GUID in request.");
        }

        UUID orionGuid = UUID.fromString(request.orionGuid);

        // validate credit belongs to shopper
        getAndValidateUserAccountCredit(creditService, orionGuid, user.getShopperId());

        //  Only vps4 credits are allowed panopta installations at the moment.
        // check credit and lookup spec to ensure credit is not for a ded4 server
        verifyCreditIsForVirtualServer(creditService, orionGuid);

        // prepare a request to create panopta customer
        PanoptaCustomerRequest panoptaCustomerRequest = new PanoptaCustomerRequest(creditService, config);
        panoptaCustomerRequest.createPanoptaCustomerRequest(orionGuid);

        try {
            return panoptaService.createPanoptaCustomer(panoptaCustomerRequest);
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
    @Path("/customers/{orionGuid}")
    @ApiOperation(value = "Delete a vps4 panopta customer.",
            notes = "Delete a vps4 panopta customer.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Could not delete customer, or customer does not exist.")
    })
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public void deleteCustomer(@PathParam("orionGuid") UUID orionGuid) {
        try {
            panoptaService.deletePanoptaCustomer(
                    config.get("panopta.api.partner.customer.key.prefix") + orionGuid);
        } catch (PanoptaServiceException e) {
            logger.warn("Encountered exception while attempting to delete customer in panopta: ", e);
            throw new Vps4Exception(e.getId(), e.getMessage(), e);
        }
    }


    public static class CreateCustomerRequest {
        public String orionGuid;
    }
}
