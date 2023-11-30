package com.godaddy.vps4.web.entitlementAction;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

import io.swagger.annotations.Api;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = { "chatterbox" })

@RequiresRole(roles = { GDUser.Role.CHATTERBOX })
@Path("/api/chatterbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatterboxResource {
    private static final Logger logger = LoggerFactory.getLogger(ChatterboxResource.class);

    @POST
    @Path("/receive")
    public void chatterboxReceive(JSONObject chatterboxObject) {
        logger.info("Chatterbox payload: ");
        logger.info(chatterboxObject.toJSONString());
    }
}
