package com.godaddy.vps4.ipblacklist;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;

import com.godaddy.vps4.panopta.PanoptaApiCustomerList;
import com.godaddy.vps4.panopta.PanoptaApiCustomerRequest;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface IpBlacklistClientService {

    @GET
    @Path("/{ip}")
    JSONObject getBlacklistRecord(@PathParam("ip") String ip);

    @DELETE
    @Path("/{ip}")
    void deleteBlacklistRecord(@PathParam("ip") String ip);

    @PUT
    @Path("/{ip}")
    void createBlacklistRecord(@PathParam("ip") String ip);
}
