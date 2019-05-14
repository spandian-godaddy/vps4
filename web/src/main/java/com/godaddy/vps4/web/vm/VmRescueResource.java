package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4Rescue;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.ActionResource;
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
    private static final ObjectMapper mapper = new ObjectMapper();
    private final VmService vmService;
    private final ActionResource actionResource;

    @Inject
    public VmRescueResource(GDUser user, VmResource vmResource, ActionService actionService, CommandService commandService,
            VmService vmService, ActionResource actionResource) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmService = vmService;
        this.actionResource = actionResource;
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
        VirtualMachine vm = vmResource.getVm(vmId);
        Action action = getLatestRescueAction(vmId);
        if (action == null) {
            return null;
        }
        com.godaddy.hfs.vm.VmAction hfsAction = getHfsVmAction(vm, action);
        return readRescueCredentials(hfsAction);
    }

    private Action getLatestRescueAction(UUID vmId) {
        List<Action> actions = actionResource.getVmActionList(vmId, Arrays.asList("COMPLETE"),
                Arrays.asList("RESCUE"), null, null, 1, 0);
        Action action = null;
        action = actions.isEmpty() ? null : actions.get(0);
        return action;
    }

    private com.godaddy.hfs.vm.VmAction getHfsVmAction(VirtualMachine vm, Action action) {
        com.godaddy.hfs.vm.VmAction hfsAction;
        try {
            long hfsVmActionId = mapper.readValue(action.response, Vps4Rescue.Response.class).hfsVmActionId;
            hfsAction = vmService.getVmAction(vm.hfsVmId, hfsVmActionId);
        } catch (Exception e) {
            throw new Vps4Exception("CREDENTIALS_NOT_AVAILABLE", "Unable to get rescue credentials", e);
        }
        return hfsAction;
    }

    private RescueCredentials readRescueCredentials(com.godaddy.hfs.vm.VmAction hfsAction) {
        RescueCredentials credentials;
        try {
            credentials = mapper.readValue(hfsAction.resultset, RescueCredentials.class);
        } catch (Exception e) {
            throw new Vps4Exception("CREDENTIALS_NOT_AVAILABLE", "Failed to read rescue credentials", e);
        }
        return credentials;
    }
}
