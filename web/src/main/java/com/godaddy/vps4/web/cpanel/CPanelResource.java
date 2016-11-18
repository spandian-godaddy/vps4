package com.godaddy.vps4.web.cpanel;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.cpanel.CpanelClient.CpanelServiceType;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CPanelResource {

    private static final Logger logger = LoggerFactory.getLogger(CPanelResource.class);

    final Vps4CpanelService cpanelService;

    @Inject
    public CPanelResource(Vps4CpanelService cpanelService) {
        this.cpanelService = cpanelService;
    }

    @GET
    @Path("{vmId}/cpanel/session")
    public CPanelSession getCPanelSession(@PathParam("vmId") long vmId) {
        return cpanelService.createSession(vmId, "root", CpanelServiceType.whostmgrd);
    }

    @GET
    @Path("/{vmId}/cpanel/accounts")
    public List<CPanelAccount> listCpanelAccounts(@PathParam("vmId") long vmId) {

        logger.info("GET listCpanelAccounts for VM: {}", vmId);

        return cpanelService.listCpanelAccounts(vmId);
    }
}
