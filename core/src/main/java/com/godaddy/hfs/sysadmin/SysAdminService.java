package com.godaddy.hfs.sysadmin;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/api/v1/sysadmin")
@Consumes({"application/json"})
@Produces({"application/json"})

public interface SysAdminService {
    @POST
    @Path("changePassword")
    SysAdminAction changePassword(@QueryParam("serverId") long var1,
                                                        @QueryParam("username") String var3,
                                                        @QueryParam("password") String var4,
                                                        @QueryParam("controlPanel") String var5,
                                                        ChangePasswordRequestBody var6);

    @POST
    @Path("server/{serverId}/{username}/enableAdmin")
    SysAdminAction enableAdmin(@PathParam("serverId") long var1,
                               @PathParam("username") String var3);

    @POST
    @Path("server/{serverId}/{username}/disableAdmin")
    SysAdminAction disableAdmin(@PathParam("serverId") long var1,
                                @PathParam("username") String var3);


    @POST
    @Path("changeHostname")
    SysAdminAction changeHostname(@QueryParam("serverId") long var1,
                                  @QueryParam("hostname") String var3,
                                  @QueryParam("controlPanel") String var4);

    @GET
    @Path("actions/{sysAdminActionId}")
    SysAdminAction getSysAdminAction(@PathParam("sysAdminActionId") long var1);

    @POST
    @Path("server/{serverId}/configureMTA")
    SysAdminAction configureMTA(@PathParam("serverId") long var1,
                                @QueryParam("controlPanel") String var3);

    @POST
    @Path("addUser")
    SysAdminAction addUser(@QueryParam("serverId") long var1,
                           @QueryParam("username") String var3,
                           @QueryParam("password") String var4,
                           AddUserRequestBody var5);

    @POST
    @Path("removeUser")
    SysAdminAction removeUser(@QueryParam("serverId") long var1,
                              @QueryParam("username") String var3);

    @POST
    @Path("updateNydus")
    SysAdminAction updateNydus(@QueryParam("serverId") long var1,
                               @QueryParam("updateType") String var3,
                               @QueryParam("updateVersion") String var4);

    @POST
    @Path("server/{serverId}/panopta")
    SysAdminAction installPanopta(@PathParam("serverId") long var1,
                                  @QueryParam("customerKey") String var3,
                                  @QueryParam("templateIds") String var4,
                                  @QueryParam("serverName") String var5,
                                  @QueryParam("serverKey") String var6,
                                  @QueryParam("fqdn") String var7,
                                  @QueryParam("disableServerMatch") boolean var8);

    @DELETE
    @Path("server/{serverId}/panopta")
    SysAdminAction deletePanopta(@PathParam("serverId") long var1);

    @POST
    @Path("server/{serverId}/enableWinexe")
    SysAdminAction enableWinexe(@PathParam("serverId") long var1);
}
