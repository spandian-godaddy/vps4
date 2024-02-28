package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.Bootscript;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.nydus.UpgradeNydus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NydusResource {

    private final VmResource vmResource;
    private final VmService vmService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;

    @Inject
    public NydusResource(VmResource vmResource,
                         VmService vmService,
                         ActionService actionService,
                         CommandService commandService,
                         GDUser user) {
        this.vmResource = vmResource;
        this.vmService = vmService;
        this.actionService = actionService;
        this.commandService = commandService;
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

    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD})
    @POST
    @Path("/{vmId}/upgradeNydus")
    @ApiOperation(value = "Upgrade Nydus on a VM")
    public VmAction upgradeNydus(@PathParam("vmId") UUID vmId, @QueryParam("version") String version) {
        if (!user.isAdmin() && !user.isShopper()) throw new Vps4NoShopperException();

        VirtualMachine vm = vmResource.getVm(vmId);

        UpgradeNydus.Request request = new UpgradeNydus.Request();
        request.vmId = vm.vmId;
        request.hfsVmId = vm.hfsVmId;
        request.version = version;

        return createActionAndExecute(actionService, commandService, vm.vmId,
                ActionType.UPDATE_NYDUS, request, "UpgradeNydus", user);
    }
}
