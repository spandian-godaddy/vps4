package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.move.VmMoveImageMapService;
import com.godaddy.vps4.move.VmMoveSpecMapService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.vm.Vps4MoveIn;
import com.godaddy.vps4.orchestration.vm.Vps4MoveOut;
import com.godaddy.vps4.orchestration.vm.Vps4MoveBack;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.InsertVirtualMachineParameters;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
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
    private final VmMoveSpecMapService vmMoveSpecMapService;
    private final VmMoveImageMapService vmMoveImageMapService;
    private final ImageService imageService;
    private final GDUser gdUser;
    private final Config config;
    private final CreditService creditService;

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
                                     VmMoveSpecMapService vmMoveSpecMapService,
                                     VmMoveImageMapService vmMoveImageMapService,
                                     ImageService imageService,
                                     GDUser gdUser,
                                     Config config,
                                     CreditService creditService) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.virtualMachineService = virtualMachineService;
        this.projectService = projectService;
        this.networkService = networkService;
        this.actionResource = actionResource;
        this.panoptaDataService = panoptaDataService;
        this.vmUserService = vmUserService;
        this.vps4UserService = vps4UserService;
        this.vmMoveSpecMapService = vmMoveSpecMapService;
        this.vmMoveImageMapService = vmMoveImageMapService;
        this.imageService = imageService;
        this.gdUser = gdUser;
        this.config = config;
        this.creditService = creditService;
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
    public VmAction moveIn(MoveInRequest moveInRequest) {
        int dataCenterId = Integer.parseInt(config.get("vps4.datacenter.defaultId"));

        VirtualMachine vm = insertDatabaseRecords(moveInRequest.moveInInfo, moveInRequest.moveOutInfo, dataCenterId);

        return runMoveInCommand(moveInRequest.moveOutInfo, vm);
    }

    @POST
    @RequiresRole(roles = { GDUser.Role.ADMIN })
    @Path("/{vmId}/move/back")
    public VmAction moveBack(@PathParam("vmId") UUID vmId,
                             @ApiParam(required = true) @QueryParam("orionGuid") UUID orionGuid) {
        int dcId = Integer.parseInt(config.get("vps4.datacenter.defaultId"));
        Vps4MoveBack.Request moveBackRequest = new Vps4MoveBack.Request();
        moveBackRequest.vmId = vmId;
        moveBackRequest.dcId = dcId;
        moveBackRequest.orionGuid = orionGuid;

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
        info.panoptaDetail = panoptaDataService.getPanoptaDetails(vm.vmId);
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
            vmUserService.createUser(moveOutInfo.vmUser.username, vm.vmId);
            insertPanoptaRecords(moveOutInfo, vm);
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
                moveOutInfo.hostname);
        return virtualMachineService.insertVirtualMachine(parameters);
    }

    private void insertIpAddresses(MoveOutInfo moveOutInfo, VirtualMachine virtualMachine) {
        networkService.createIpAddress(0, virtualMachine.vmId, moveOutInfo.primaryIpAddress.ipAddress, IpAddress.IpAddressType.PRIMARY);
        if (moveOutInfo.additionalIps != null && !moveOutInfo.additionalIps.isEmpty()) {
            for (IpAddress ipAddress : moveOutInfo.additionalIps) {
                networkService.createIpAddress(ipAddress.hfsAddressId, virtualMachine.vmId, ipAddress.ipAddress, IpAddress.IpAddressType.SECONDARY);
            }
        }
    }

    private void insertPanoptaRecords(MoveOutInfo moveOutInfo, VirtualMachine vm) {
        panoptaDataService.createOrUpdatePanoptaCustomerFromKey(moveOutInfo.panoptaDetail.getPartnerCustomerKey(), moveOutInfo.panoptaDetail.getCustomerKey());
        panoptaDataService.insertPanoptaServerFromKey(
                vm.vmId,
                moveOutInfo.panoptaDetail.getPartnerCustomerKey(),
                moveOutInfo.panoptaDetail.getServerId(),
                moveOutInfo.panoptaDetail.getServerKey(),
                moveOutInfo.panoptaDetail.getTemplateId());
    }

    private void markNewRecordsDeleted(VirtualMachine vm) {
        if (vm != null) {
            vm = virtualMachineService.getVirtualMachine(vm.vmId);
            virtualMachineService.setVmCanceled(vm.vmId);
            virtualMachineService.setVmRemoved(vm.vmId);
            panoptaDataService.setPanoptaServerDestroyed(vm.vmId);

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
        moveInRequest.vm = vm;
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
