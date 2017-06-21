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

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
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

    @Inject
    public CreditResource(GDUser user, CreditService creditService) {
        this.user = user;
        this.creditService = creditService;
    }

    @GET
    @Path("/{orionGuid}")
    public VirtualMachineCredit getCredit(@PathParam("orionGuid") UUID orionGuid) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        if (user.isShopper())
            if (credit == null || !credit.shopperId.equals(user.getShopperId())) {
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
            return creditService.getActiveVirtualMachineCredits(user.getShopperId());
        }
        return creditService.getUnclaimedVirtualMachineCredits(user.getShopperId());
    }

    @POST
    @Path("/trialCredit")
    public VirtualMachineCredit createTrialCredit(){
        if(!user.isShopper()){
            throw new Vps4NoShopperException();
        }
        List<VirtualMachineCredit> existingCredits = creditService.getActiveVirtualMachineCredits(user.getShopperId());
        if (!existingCredits.isEmpty()){
            // only one credit at a time
            throw new Vps4Exception("CREDIT_ALREADY_EXISTS", "This shopper already has existing credits");
        }

        UUID orionGuid = UUID.randomUUID();
        String os = "Linux";
        String cp = ControlPanel.CPANEL.toString();
        int tier = 20;
        int managedLevel = 2;
        int monitoring = 1;
        creditService.createVirtualMachineCredit(orionGuid, os, cp, tier, managedLevel, monitoring, user.getShopperId());
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        return credit;
    }

    @AdminOnly
    @POST
    @Path("/")
    public VirtualMachineCredit createCredit(CreateCreditRequest request){
        UUID orionGuid = UUID.randomUUID();

        creditService.createVirtualMachineCredit(orionGuid,
                request.operatingSystem, request.controlPanel,
                request.tier, request.managedLevel, request.monitoring, request.shopperId);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        return credit;
    }

    public static class CreateCreditRequest {
        public int tier;
        public int managedLevel;
        public int monitoring;
        public String operatingSystem;
        public String controlPanel;
        public String shopperId;
    }

    @AdminOnly
    @POST
    @Path("/{orionGuid}/release")
    public VirtualMachineCredit releaseCredit(@PathParam("orionGuid") UUID orionGuid) {
        creditService.unclaimVirtualMachineCredit(orionGuid);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        return credit;
    }
}