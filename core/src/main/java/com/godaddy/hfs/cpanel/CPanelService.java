package com.godaddy.hfs.cpanel;

import gdg.hfs.request.CompleteResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api/v1/cpanel")
@Api(tags = { "cpanel" })

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CPanelService {
    @POST
    @Path("/access")
    @ApiOperation( value = "Request access to cPanel VM. SSO token will be available in CPanelAction.responsePayload", notes = "", code=202)
    CPanelAction requestAccess(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to prepare cPanel on") @QueryParam("serverId") long serverId,
            @ApiParam(name = "publicIP", required = true, value = "The public IP of the cPanel VM request access to") @QueryParam("publicIP") String publicIP);
    @POST
    @Path("/apiToken")
    @ApiOperation( value = "Request api token to cPanel VM. Api token will be available in CPanelAction.responsePayload", notes = "", code=202)
    CPanelAction requestApiToken(
            @ApiParam(name = "serverId", required = true, value = "The ID of the VM to get an API token from") @QueryParam("serverId") long serverId);
    @POST
    @Path("/siteList")
    @ApiOperation( value = "Retrieve the list of sites on a cPanel VM.  List will be available in CPanelAction.responsePayload", notes = "", code=202)
    CPanelAction requestSiteList(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to prepare cPanel on") @QueryParam("serverId") long serverId,
            @ApiParam(name = "fromIP", required = false, value = "The IP which the user will access the target VM from") @QueryParam("fromIP") String fromIP);

    // Get a single CPanelAction by its Id
    @GET
    @Path("/actions/{cPanelActionId}")
    CPanelAction getAction(
            @ApiParam(name = "cPanelActionId", required = true, value = "ID of the action to get") @PathParam("cPanelActionId") long cPanelActionId);

    @POST
    @Path("/imagePrep")
    @ApiOperation( value = "Prepare a VM with cPanel.  This is done before a snapshot is created for publishing", notes = "", code=202)
    CPanelAction imagePrep(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to prepare cPanel on") @QueryParam("serverId") long serverId );

    @POST
    @Path("/imageConfig")
    @ApiOperation( value = "Configure a VM with cPanel.  This is done to a customers VM just before releasing it to them", notes = "", code=202)
    CPanelAction imageConfig(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to configure cPanel on") @QueryParam("serverId") long serverId);

    @POST
    @Path("/getcPanelPublicIp")
    @ApiOperation( value = "Retrieve cPanel public IP.", notes = "", code=202)
    CPanelAction getcPanelPublicIp(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to configure cPanel on") @QueryParam("serverId") long serverId);

    
    @POST
    @Path("/licenseRefresh")
    @ApiOperation( value = "Refresh the license on a cPanel VM.", code=202)
    CPanelAction licenseRefresh(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to configure cPanel on") @QueryParam("serverId") long serverId);

    @POST
    @Path("/licenseActivate")
    @ApiOperation( value = "Activate the license on a cPanel VM.", code=202)
    CPanelAction licenseActivate(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to configure cPanel on") @QueryParam("serverId") long serverId);

    @POST
    @Path("/licenseRelease")
    @ApiOperation( value = "Release the license on a cPanel VM either by vmID or licensedIP.", code=202)
    CPanelAction licenseRelease(
            @ApiParam(name = "licenseIP", required = false, value = "The IP address of the license to release") @QueryParam("licenseIP") String licenseIP,
            @ApiParam(name = "serverId", required = false, value = "The ID of the vm to configure cPanel on") @QueryParam("serverId") long serverId);


    @GET
    @Path("/getLicenseFromDb")
    CPanelLicense getLicenseFromDb(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to retrieve license from") @QueryParam("serverId") long serverId);

    @POST
    @Path("/licenseUpdateIP")
    @ApiOperation( value = "Update/Change the IP for the license on a cPanel VM.", code=202)
    CPanelAction licenseUpdateIP(
            @ApiParam(name = "serverId", required = true, value = "The ID of the vm to configure cPanel on") @QueryParam("serverId") long serverId,
            @ApiParam(name = "oldIP", required = true, value = "The public IP to be released from a cPanel license") @QueryParam("oldIP") String oldIP,
            @ApiParam(name = "newIP", required = true, value = "The public IP to be activated for a cPanel license") @QueryParam("newIP") String newIP);

	@POST
	@Path("onComplete")
	@Consumes(MediaType.APPLICATION_JSON)
	void onComplete(CompleteResponse response);
}
