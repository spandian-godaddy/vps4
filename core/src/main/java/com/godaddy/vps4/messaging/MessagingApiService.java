package com.godaddy.vps4.messaging;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.messaging.models.MessagingResponse;
import com.godaddy.vps4.messaging.models.ShopperMessage;

@Path("/v1/messaging")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface MessagingApiService {
    @POST
    @Path("/messages")
    MessagingResponse sendMessage(@HeaderParam("authorization") String auth,
                                  @HeaderParam("x-shopper-id") String shopperId,
                                  ShopperMessage request);
}
