package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.Bootscript;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmBootscriptResource {

    private final VmResource vmResource;
    private final VmService vmService;
    private final GDUser user;

    @Inject
    public VmBootscriptResource(VmResource vmResource, VmService vmService, GDUser user) {
        this.vmResource = vmResource;
        this.vmService = vmService;
        this.user = user;
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD})
    @GET
    @Path("/{vmId}/bootscript")
    @ApiOperation(value = "get HFS nydus bootscript for Openstack Vms")
    public Bootscript getBootscript(@PathParam("vmId") UUID vmId) {
        if (!user.isAdmin() && !user.isShopper()) { // HS Leads must impersonate shopper to generate bootscript
            throw new Vps4NoShopperException();
        }

        VirtualMachine vm = vmResource.getVm(vmId); // auth validation

        return vmService.getBootscript(vm.hfsVmId, vm.hostname, true);
    }
}
