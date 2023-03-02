package com.godaddy.vps4.sso;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.sso.models.Vps4SsoToken;

@Path("/v1/secure/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface Vps4SsoService {
    @POST
    @Path("/token")
    Vps4SsoToken getToken(@FormParam("realm") String realm);
}
