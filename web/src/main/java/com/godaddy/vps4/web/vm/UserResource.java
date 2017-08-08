package com.godaddy.vps4.web.vm;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    final VmUserService userService;
    final VmResource vmResource;

    @Inject
    public UserResource(VmUserService userService, VirtualMachineService vmService, VmResource vmResource) {
        this.userService = userService;
        this.vmResource = vmResource;
    }

    @GET
    @Path("/{vmId}/users")
    public List<VmUser> getUsers(@PathParam("vmId") UUID vmId, @QueryParam("type") VmUserType type) {
        VirtualMachine vm = vmResource.getVm(vmId);
        return userService.listUsers(vm.vmId, type);
    }
}
