package com.godaddy.hfs.plesk;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/api/v1/plesk")
@Consumes({"application/json"})
@Produces({"application/json"})
public interface PleskService {

    // Get a single PleskAction by its Id
    @GET
    @Path("/actions/{PleskActionId}")
    PleskAction getAction(@PathParam("PleskActionId") long PleskActionId);

    @POST
    @Path("/imageConfig")
    PleskAction imageConfig(@QueryParam("serverId") long var1,
                            @QueryParam("pleskUser") String var3,
                            @QueryParam("pleskPass") String var4,
                            @QueryParam("pleskLicenseType") String var5);

    @POST
    @Path("/licenseRelease")
    PleskAction licenseRelease(@QueryParam("serverId") long var1);

    @POST
    @Path("/setOutgoingEMailIP")
    PleskAction setOutgoingEMailIP(@QueryParam("serverId") long serverId, @QueryParam("address") String address);

    @POST
    @Path("/siteList")
    PleskAction requestSiteList(@QueryParam("serverId") long serverId);

    @POST
    @Path("/adminPassUpdate")
    PleskAction adminPassUpdate(@QueryParam("serverId") long serverId, @QueryParam("pleskAdminPass") String pleskAdminPass);

    @POST
    @Path("/access")
    PleskAction requestAccess(@QueryParam("serverId") long serverId, @QueryParam("fromIP") String fromIP);

}
