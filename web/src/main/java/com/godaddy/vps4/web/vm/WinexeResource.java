package com.godaddy.vps4.web.vm;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.VmHelper;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {Role.ADMIN})
public class WinexeResource {
    private final GDUser user;
    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;

    @Inject
    public WinexeResource(GDUser user, VmResource vmResource, ActionService actionService, CommandService commandService) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
    }

    @POST
    @Path("/{vmId}/enableWinexe")
    public VmAction enableWinexe(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        if (vm.image.operatingSystem != Image.OperatingSystem.WINDOWS) {
            String errMsg = "Only Windows servers can call this endpoint. Found: %s";
            throw new Vps4Exception("INVALID_IMAGE", String.format(errMsg, vm.image.operatingSystem));
        }
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return VmHelper.createActionAndExecute(actionService, commandService, vmId,
                                               ActionType.ENABLE_WINEXE, request, "Vps4EnableWinexe", user);
    }
}
