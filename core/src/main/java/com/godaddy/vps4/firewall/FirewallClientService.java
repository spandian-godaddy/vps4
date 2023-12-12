package com.godaddy.vps4.firewall;


import com.godaddy.vps4.firewall.model.FirewallDestroyResponse;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;

import javax.ws.rs.*;
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
    FirewallDestroyResponse deleteFirewallSite(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId);

}
