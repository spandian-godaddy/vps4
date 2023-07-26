package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.vm.Vps4MoveOut;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlatformMigrationResource {
    private final ActionService actionService;
    private final CommandService commandService;
    private final VirtualMachineService virtualMachineService;
    private final ProjectService projectService;
    private final NetworkService networkService;
    private final ActionResource actionResource;
    private final PanoptaDataService panoptaDataService;
    private final VmUserService vmUserService;
    private final Vps4UserService vps4UserService;
    private final GDUser gdUser;

    @Inject
    public PlatformMigrationResource(ActionService actionService,
                                     CommandService commandService,
                                     VirtualMachineService virtualMachineService,
                                     ProjectService projectService,
                                     NetworkService networkService,
                                     ActionResource actionResource,
                                     PanoptaDataService panoptaDataService,
                                     VmUserService vmUserService,
                                     Vps4UserService vps4UserService,
                                     GDUser gdUser) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.virtualMachineService = virtualMachineService;
        this.projectService = projectService;
        this.networkService = networkService;
        this.actionResource = actionResource;
        this.panoptaDataService = panoptaDataService;
        this.vmUserService = vmUserService;
        this.vps4UserService = vps4UserService;
        this.gdUser = gdUser;
    }

    @POST
    @RequiresRole(roles = { GDUser.Role.ADMIN })
    @Path("/{vmId}/move/out")
    public MoveOutInfo moveOut(@PathParam("vmId") UUID vmId) {
        // TODO: Call intervention endpoint

        Vps4MoveOut.Request moveOutRequest = new Vps4MoveOut.Request();
        moveOutRequest.vmId = vmId;
        VmAction action = createActionAndExecute(actionService, commandService, vmId, ActionType.MOVE_OUT, moveOutRequest,
                "Vps4MoveOut", gdUser);

        MoveOutInfo info = getInfo(vmId);
        info.commandId = action.commandId;

        return info;
    }

    private MoveOutInfo getInfo(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        MoveOutInfo info = new MoveOutInfo();
        info.entitlementId = vm.orionGuid;
        info.serverName = vm.name;
        info.spec = vm.spec;
        info.image = vm.image;
        info.hostname = vm.hostname;
        info.project = projectService.getProject(vm.projectId);
        info.primaryIpAddress = vm.primaryIpAddress;
        info.additionalIps = networkService.getVmSecondaryAddress(vm.hfsVmId);
        info.actions = getActions(vmId);
        info.panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        info.vmUser = vmUserService.getPrimaryCustomer(vmId);
        info.vps4User = vps4UserService.getUser(gdUser.getShopperId());

        return info;
    }

    private List<Action> getActions(UUID vmId) {
        List<String> statusList = Arrays.stream(ActionStatus.values()).map(Enum::name).collect(Collectors.toList());
        List<String> typeList = Arrays.stream(ActionType.values()).map(Enum::name).collect(Collectors.toList());

        return actionResource.getActionList(ActionResource.ResourceType.VM, vmId, statusList, typeList,
                null, null, Long.MAX_VALUE, 0);
    }
}
