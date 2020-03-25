package com.godaddy.vps4.web.monitoring;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmAddMonitoringResource {

    private final GDUser user;
    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;

    @Inject
    public VmAddMonitoringResource(GDUser user, VmResource vmResource, ActionService actionService,
            CommandService commandService) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
    }

    @POST
    @Path("/{vmId}/monitoring/install")
    @ApiOperation(value = "Install monitoring agent on customer server")
    public VmAction installPanoptaAgent(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        validateServerIsActive(vmResource.getVmFromVmVertical(vm.hfsVmId));
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.ADD_MONITORING);

        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.ADD_MONITORING, request, "Vps4AddMonitoring", user);
    }

}
