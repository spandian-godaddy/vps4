package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.move.VmMoveImageMapService;
import com.godaddy.vps4.move.VmMoveSpecMapService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4MoveIn;
import com.godaddy.vps4.orchestration.vm.Vps4MoveOut;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlatformMigrationResource {
    private final ActionResource actionResource;
    private final GDUser gdUser;
    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final Config config;
    private final CreditService creditService;
    private final ImageService imageService;
    private final NetworkService networkService;
    private final ProjectService projectService;
    private final VirtualMachineService virtualMachineService;
    private final VmMoveImageMapService vmMoveImageMapService;
    private final VmMoveSpecMapService vmMoveSpecMapService;
    private final VmUserService vmUserService;
    private final Vps4UserService vps4UserService;

    @Inject
    public PlatformMigrationResource(ActionResource actionResource,
                                     GDUser gdUser,
                                     VmResource vmResource,
                                     ActionService actionService,
                                     CommandService commandService,
                                     Config config,
                                     CreditService creditService,
                                     ImageService imageService,
                                     NetworkService networkService,
                                     ProjectService projectService,
                                     VirtualMachineService virtualMachineService,
                                     VmMoveImageMapService vmMoveImageMapService,
                                     VmMoveSpecMapService vmMoveSpecMapService,
                                     VmUserService vmUserService,
                                     Vps4UserService vps4UserService) {
        this.actionResource = actionResource;
        this.gdUser = gdUser;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.config = config;
        this.creditService = creditService;
        this.imageService = imageService;
        this.networkService = networkService;
        this.projectService = projectService;
        this.virtualMachineService = virtualMachineService;
        this.vmMoveImageMapService = vmMoveImageMapService;
        this.vmMoveSpecMapService = vmMoveSpecMapService;
        this.vmUserService = vmUserService;
        this.vps4UserService = vps4UserService;
    }

    @POST
    @RequiresRole(roles = { GDUser.Role.ADMIN })
    @Path("/{vmId}/move/out")
    public MoveOutInfo moveOut(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        MoveOutInfo info = getInfo(vm);

        Vps4MoveOut.Request moveOutRequest = new Vps4MoveOut.Request();
        moveOutRequest.vmId = vmId;
        moveOutRequest.backupJobId = vm.backupJobId;
        moveOutRequest.hfsVmId = vm.hfsVmId;
        moveOutRequest.addressIds = new ArrayList<>();
        moveOutRequest.addressIds.add(vm.primaryIpAddress.addressId);
        moveOutRequest.addressIds.addAll(info.additionalIps.stream().map(ipAddress -> ipAddress.addressId).collect(Collectors.toList()));
        for (IpAddress address : info.additionalIps) {
            moveOutRequest.addressIds.add(address.addressId);
        }

        createActionAndExecute(actionService, commandService, vmId, ActionType.MOVE_OUT, moveOutRequest,
                "Vps4MoveOut", gdUser);

        return info;
    }

    @POST
    @RequiresRole(roles = { GDUser.Role.ADMIN })
    @Path("/move/in")
    public VmAction moveIn(MoveInRequest request) {
        int dataCenterId = Integer.parseInt(config.get("vps4.datacenter.defaultId"));

        VirtualMachine vm = insertDatabaseRecords(request.moveInInfo, request.moveOutInfo, dataCenterId);

        return runMoveInCommand(request.moveOutInfo, vm);
    }

    @POST
    @RequiresRole(roles = { GDUser.Role.ADMIN })
    @Path("/{vmId}/move/back")
    public VmAction moveBack(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        VmActionRequest moveBackRequest = new VmActionRequest();
        moveBackRequest.virtualMachine = vm;

        return createActionAndExecute(actionService, commandService, vmId, ActionType.MOVE_BACK, moveBackRequest,
                "Vps4MoveBack", gdUser);
    }

    private MoveOutInfo getInfo(VirtualMachine vm) {
        MoveOutInfo info = new MoveOutInfo();
        info.entitlementId = vm.orionGuid;
        info.serverName = vm.name;
        info.specName = vm.spec.specName;
        info.hfsImageName = vm.image.hfsName;
        info.hostname = vm.hostname;
        info.primaryIpAddress = vm.primaryIpAddress;
        info.additionalIps = networkService.getVmActiveSecondaryAddresses(vm.hfsVmId);
        info.actions = getActions(vm.vmId);
        info.vmUser = vmUserService.getPrimaryCustomer(vm.vmId);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        info.vps4User = vps4UserService.getUser(credit.getShopperId());

        return info;
    }

    private List<Action> getActions(UUID vmId) {
        List<String> statusList = Arrays.stream(ActionStatus.values()).map(Enum::name).collect(Collectors.toList());
        List<String> typeList = Arrays.stream(ActionType.values()).map(Enum::name).collect(Collectors.toList());

        return actionResource.getActionList(ActionResource.ResourceType.VM, vmId, statusList, typeList,
                null, null, Long.MAX_VALUE, 0);
    }


    private VirtualMachine insertDatabaseRecords(MoveInInfo moveInInfo, MoveOutInfo moveOutInfo, int dataCenterId) {
        VirtualMachine vm = null;
        try {
            Vps4User vps4User = vps4UserService.getOrCreateUserForShopper(
                    moveOutInfo.vps4User.getShopperId(),
                    moveOutInfo.vps4User.getResellerId(),
                    moveOutInfo.vps4User.getCustomerId());

            Project project = projectService.createProject(
                    moveOutInfo.entitlementId.toString(),
                    vps4User.getId(),
                    moveInInfo.sgid);

            vm = insertVirtualMachine(moveInInfo, moveOutInfo, dataCenterId, project);
            insertIpAddresses(moveOutInfo, vm);
            vmUserService.createUser(moveOutInfo.vmUser.username, vm.vmId, moveOutInfo.vmUser.adminEnabled);
        } catch (Exception e) {
            markNewRecordsDeleted(vm);
            throw new Vps4Exception("MOVE_IN_FAILED", e.getMessage(), e);
        }
        return vm;
    }

    private VirtualMachine insertVirtualMachine(MoveInInfo moveInInfo, MoveOutInfo moveOutInfo, int dataCenterId, Project project) {
        ServerSpec fromSpec = virtualMachineService.getSpec(moveOutInfo.specName);
        ServerSpec toSpec = virtualMachineService.getSpec(vmMoveSpecMapService.getVmMoveSpecMap(fromSpec.specId, moveInInfo.platform).toSpecId);
        Image fromImage = imageService.getImageByHfsName(moveOutInfo.hfsImageName);
        Image toImage = imageService.getImage(vmMoveImageMapService.getVmMoveImageMap(fromImage.imageId, moveInInfo.platform).toImageId);

        InsertVirtualMachineParameters parameters = new InsertVirtualMachineParameters(
                moveInInfo.hfsVmId,
                moveOutInfo.entitlementId,
                moveOutInfo.serverName,
                project.getProjectId(),
                toSpec.specId,
                toImage.imageId,
                dataCenterId,
                moveOutInfo.hostname,
                toImage.imageName);
        return virtualMachineService.insertVirtualMachine(parameters);
    }

    private void insertIpAddresses(MoveOutInfo moveOutInfo, VirtualMachine vm) {
        vm.primaryIpAddress = networkService.createIpAddress(0,
                                                             vm.vmId,
                                                             moveOutInfo.primaryIpAddress.ipAddress,
                                                             IpAddress.IpAddressType.PRIMARY);
        if (moveOutInfo.additionalIps != null && !moveOutInfo.additionalIps.isEmpty()) {
            for (IpAddress ipAddress : moveOutInfo.additionalIps) {
                networkService.createIpAddress(ipAddress.hfsAddressId,
                                               vm.vmId,
                                               ipAddress.ipAddress,
                                               IpAddress.IpAddressType.SECONDARY);
            }
        }
    }

    private void markNewRecordsDeleted(VirtualMachine vm) {
        if (vm != null) {
            virtualMachineService.setVmCanceled(vm.vmId);
            virtualMachineService.setVmRemoved(vm.vmId);

            if (vm.primaryIpAddress != null) networkService.destroyIpAddress(vm.primaryIpAddress.addressId);
            List<IpAddress> additionalIps = networkService.getVmActiveSecondaryAddresses(vm.hfsVmId);
            if (additionalIps != null && !additionalIps.isEmpty()) {
                for (IpAddress ipAddress : additionalIps) {
                    networkService.destroyIpAddress(ipAddress.addressId);
                }
            }
        }
    }

    private VmAction runMoveInCommand(MoveOutInfo moveOutInfo, VirtualMachine vm) {
        Vps4MoveIn.Request moveInRequest = new Vps4MoveIn.Request();
        moveInRequest.virtualMachine = vm;
        moveInRequest.actions = moveOutInfo.actions;

        return createActionAndExecute(
                actionService,
                commandService,
                vm.vmId,
                ActionType.MOVE_IN,
                moveInRequest,
                "Vps4MoveIn",
                gdUser);
    }
}
