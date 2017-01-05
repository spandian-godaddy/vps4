package com.godaddy.vps4.web.vm;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    final VmUserService userService;
    final VirtualMachineService vmService;

    @Inject
    public UserResource(VmUserService userService, VirtualMachineService vmService){
        this.userService = userService;
        this.vmService = vmService;
    }

    @GET
    @Path("/{vmId}/users")
    public List<VmUser> getUsers(@PathParam("vmId") long vmId){
        VirtualMachine vm = vmService.getVirtualMachine(vmId);
        return userService.listUsers(vm.id);
    }
}
