package com.godaddy.vps4.web.credit;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditHistory;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "credits" })

@Path("/api/credits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreditResource {

    private static final Logger logger = LoggerFactory.getLogger(CreditResource.class);

    private final GDUser user;
    private final CreditService creditService;
    private final VmMailRelayResource vmMailRelayResource;
    private VirtualMachineService vmService;

    @Inject
    public CreditResource(GDUser user, CreditService creditService, VmMailRelayResource vmMailRelayResource, VirtualMachineService vmService) {
        this.user = user;
        this.creditService = creditService;
        this.vmMailRelayResource = vmMailRelayResource;
        this.vmService = vmService;
    }

    @GET
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.CUSTOMER, GDUser.Role.VPS4_API_READONLY,
            GDUser.Role.SUSPEND_AUTH})
    @Path("/{orionGuid}")
    public VirtualMachineCredit getCredit(@PathParam("orionGuid") UUID orionGuid) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        if (user.isShopper())
            if (credit == null || !credit.getShopperId().equals(user.getShopperId())) {
                throw new NotFoundException("Unknown Credit ID: " + orionGuid);
        }
        return credit;
    }

    @GET
    @Path("/")
    public List<VirtualMachineCredit> getCredits(@DefaultValue("false") @QueryParam("showClaimed") boolean showClaimed) {
        if (!user.isShopper())
            throw new Vps4NoShopperException();
        logger.debug("Getting credits for shopper {}", user.getShopperId());
        if(showClaimed){
            return creditService.getVirtualMachineCredits(user.getShopperId());
        }
        return creditService.getUnclaimedVirtualMachineCredits(user.getShopperId());
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @POST
    @Path("/")
    public VirtualMachineCredit createCredit(CreateCreditRequest request){
        UUID orionGuid = UUID.randomUUID();

        creditService.createVirtualMachineCredit(orionGuid, request.shopperId, request.operatingSystem, request.controlPanel,
                request.tier, request.managedLevel, request.monitoring, request.resellerId);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        return credit;
    }

    public static class CreateCreditRequest {
        public String shopperId;
        public String operatingSystem;
        public String controlPanel;
        public int tier;
        public int managedLevel;
        public int monitoring;
        public int resellerId;
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @POST
    @Path("/{orionGuid}/release")
    public VirtualMachineCredit releaseCredit(@PathParam("orionGuid") UUID orionGuid) {
        UUID vmId = UUID.fromString(
                creditService.getProductMeta(orionGuid)
                    .get(ProductMetaField.PRODUCT_ID));
        creditService.unclaimVirtualMachineCredit(orionGuid, vmId, vmMailRelayResource.getCurrentMailRelayUsage(vmId).relays);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        return credit;
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/{orionGuid}/history")
    public List<CreditHistory> getHistory(@PathParam("orionGuid") UUID orionGuid) {
        return vmService.getCreditHistory(orionGuid);
    }
}
