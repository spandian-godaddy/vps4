package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmRescueResource {

    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;

    @Inject
    public VmRescueResource(GDUser user, VmResource vmResource, ActionService actionService, CommandService commandService) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
    }

    @POST
    @Path("{vmId}/rescue")
    public VmAction rescue(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        validateServerInCompatibleMode(vm.hfsVmId, "ACTIVE");
        
        VmActionRequest rescueRequest = new VmActionRequest();
        rescueRequest.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vmId, ActionType.RESCUE, rescueRequest, 
                "Vps4Rescue", user);
    }

    @POST
    @Path("{vmId}/endRescue")
    public VmAction endRescue(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        validateServerInCompatibleMode(vm.hfsVmId, "RESCUED");

        VmActionRequest endRescueRequest = new VmActionRequest();
        endRescueRequest.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vmId, ActionType.END_RESCUE,
                endRescueRequest, "Vps4EndRescue", user);
    }

    private void validateServerInCompatibleMode(long hfsVmId, String mode) {
        Vm hfsVm = vmResource.getVmFromVmVertical(hfsVmId);
        if (!hfsVm.status.equals(mode)) {
            throw new Vps4Exception("INVALID_STATUS", String.format("The server is not in %s Mode", mode));
        }
    }

    @GET
    @Path("{vmId}/rescueCredentials")
    public RescueCredentials getRescueCredentials(@PathParam("vmId") UUID vmId) {
        return new RescueCredentials("testUsername", "testPassword");
    }
}
