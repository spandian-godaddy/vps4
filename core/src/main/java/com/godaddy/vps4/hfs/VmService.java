package com.godaddy.vps4.hfs;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/v1/vms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface VmService {

	@GET
    @Path("{vmId}/")
    Vm getVm(@PathParam("vmId") long vmId);

	@GET
    @Path("{vmId}/actions/{vmActionId}/")
    VmAction getVmAction(
    		@PathParam("vmId") 		 long vmId,
    		@PathParam("vmActionId") long vmActionId);

	@POST
    @Path("/")
    VmAction createVm(CreateVMRequest request);

	@POST
    @Path("{vmId}/destroy")
    VmAction destroyVm(@PathParam("vmId") long vmId);

	@GET
	@Path("flavors/")
	FlavorList getFlavors();

	public static class FlavorList {
		public List<Flavor> results;
	}

}
