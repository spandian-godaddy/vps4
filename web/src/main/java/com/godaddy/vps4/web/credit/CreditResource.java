package com.godaddy.vps4.web.credit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.Vps4CreditService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "credits" })

@Path("/api/credits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreditResource {

    private static final Logger logger = LoggerFactory.getLogger(CreditResource.class);

    private final Vps4User user;
    private final Vps4CreditService creditService;

    @Inject
    public CreditResource(Vps4User user, Vps4CreditService creditService) {
        this.user = user;
        this.creditService = creditService;
    }

    @GET
    @Path("/{orionGuid}")
    public VirtualMachineCredit getCredit(@PathParam("orionGuid") UUID orionGuid) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        if (credit == null || !(credit.shopperId.equals(user.getShopperId()))) {
            throw new IllegalArgumentException("Unknown Credit ID: " + orionGuid);
        }
        return credit;
    }

    @GET
    @Path("/")
    public List<VirtualMachineCredit> getCredits() {
        logger.debug("Getting credits for shopper {}", user.getShopperId());
        return creditService.getVirtualMachineCredits(user.getShopperId());
    }
}
