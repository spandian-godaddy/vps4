package com.godaddy.vps4.firewall;


import com.godaddy.vps4.PATCH;
import com.godaddy.vps4.firewall.model.FirewallClientResponse;
import com.godaddy.vps4.firewall.model.FirewallClientUpdateRequest;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;

import javax.ws.rs.GET;
import javax.ws.rs.DELETE;

import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface FirewallClientService {
    @GET
    @Path("/sites")
    List<FirewallSite> getFirewallSites(@HeaderParam("Authorization") String ssoJwt);

    @GET
    @Path("/sites/{siteId}")
    FirewallDetail getFirewallSiteDetail(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId);

    @DELETE
    @Path("/sites/{siteId}")
    FirewallClientResponse deleteFirewallSite(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId);

    @PATCH
    @Path("/sites/{siteId}")
    FirewallClientResponse modifyFirewallSite(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId, FirewallClientUpdateRequest updateFirewallRequest);


}
