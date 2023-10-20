package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.move.VmMoveImageMap;
import com.godaddy.vps4.move.VmMoveImageMapService;
import com.godaddy.vps4.move.VmMoveSpecMap;
import com.godaddy.vps4.move.VmMoveSpecMapService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

@RunWith(MockitoJUnitRunner.class)
public class PlatformMigrationResourceTest {
    @Mock private ActionResource actionResource;
    @Mock private GDUser user;
    @Mock private VmResource vmResource;
    @Mock private ActionService actionService;
    @Mock private CommandService commandService;
    @Mock private Config config;
    @Mock private CreditService creditService;
    @Mock private ImageService imageService;
    @Mock private NetworkService networkService;
    @Mock private ProjectService projectService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private VmMoveImageMapService vmMoveImageMapService;
    @Mock private VmMoveSpecMapService vmMoveSpecMapService;
    @Mock private VmUserService vmUserService;
    @Mock private Vps4UserService vps4UserService;

    private final UUID vmId = UUID.randomUUID();
    private final List<IpAddress> additionalIps = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private final VmUser vmUser = mock(VmUser.class);
    private final Action moveOutAction = mock(Action.class);

    private VirtualMachine vm;
    private Project project;
    private Vps4User vps4User;
    private MoveOutInfo moveOutInfo;
    private MoveInInfo moveInInfo;
    private MoveInRequest moveInRequest;
    private VirtualMachineCredit credit;

    @Captor private ArgumentCaptor<InsertVirtualMachineParameters> insertVirtualMachineParametersCaptor;

    private PlatformMigrationResource resource;

    @Before
    public void setup() {
        vm = new VirtualMachine();
        vm.vmId = vmId;
        vm.orionGuid = UUID.randomUUID();
        vm.name = "mock-vm";
        vm.spec = mock(ServerSpec.class);
        vm.spec.serverType = mock(ServerType.class);
        vm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        vm.image = mock(Image.class);
        vm.image.serverType = mock(ServerType.class);
        vm.image.serverType.platform = ServerType.Platform.OPENSTACK;
        vm.hostname = "fake-hostname";
        vm.projectId = 12345L;
        vm.primaryIpAddress = mock(IpAddress.class);
        vm.hfsVmId = 67890L;

        vps4User = new Vps4User(12, "test-vps4-shopper", UUID.randomUUID(), "1");
        project = new Project(vm.projectId, "testProject", "testSgid", Instant.now(), Instant.MAX, vps4User.getId());

        getMoveOutInfo();
        moveInInfo = new MoveInInfo();
        moveInInfo.platform = ServerType.Platform.OPENSTACK;
        moveInInfo.sgid = "testMoveIn";
        moveInInfo.hfsVmId = 132;

        moveOutAction.commandId = UUID.randomUUID();

        moveInRequest = new MoveInRequest();
        moveInRequest.moveOutInfo = moveOutInfo;
        moveInRequest.moveInInfo = moveInInfo;

        credit = mock(VirtualMachineCredit.class);
        when(credit.getShopperId()).thenReturn(vps4User.getShopperId());

        when(actionService.getAction(anyLong())).thenReturn(moveOutAction);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        when(projectService.getProject(vm.projectId)).thenReturn(project);
        when(networkService.getVmActiveSecondaryAddresses(vm.hfsVmId)).thenReturn(additionalIps);
        when(actionResource.getActionList(null, null, null, null, null,
                null, Long.MAX_VALUE, 0)).thenReturn(actions);
        when(vmUserService.getPrimaryCustomer(vmId)).thenReturn(vmUser);
        when(vps4UserService.getUser(vps4User.getShopperId())).thenReturn(vps4User);
        when(config.get("vps4.datacenter.defaultId")).thenReturn("1");
        when(vps4UserService.getOrCreateUserForShopper(
                moveOutInfo.vps4User.getShopperId(),
                moveOutInfo.vps4User.getResellerId(),
                moveOutInfo.vps4User.getCustomerId()))
                .thenReturn(vps4User);

        VmMoveImageMap vmMoveImageMap = new VmMoveImageMap();
        vmMoveImageMap.fromImageId = vm.image.imageId;
        vmMoveImageMap.toImageId = vm.image.imageId + 10;
        vmMoveImageMap.id = 1;
        when(vmMoveImageMapService.getVmMoveImageMap(vm.image.imageId, ServerType.Platform.OPENSTACK))
                .thenReturn(vmMoveImageMap);
        when(imageService.getImageByHfsName(vm.image.hfsName)).thenReturn(vm.image);
        when(imageService.getImage(vmMoveImageMap.toImageId)).thenReturn(vm.image);

        VmMoveSpecMap vmMoveSpecMap = new VmMoveSpecMap();
        vmMoveSpecMap.fromSpecId = vm.spec.specId;
        vmMoveSpecMap.toSpecId = vm.spec.specId + 5;
        vmMoveSpecMap.id = 1;
        when(vmMoveSpecMapService.getVmMoveSpecMap(vm.spec.specId, ServerType.Platform.OPENSTACK))
                .thenReturn(vmMoveSpecMap);
        when(virtualMachineService.getSpec(vm.spec.specName)).thenReturn(vm.spec);
        when(virtualMachineService.getSpec(vmMoveSpecMap.toSpecId)).thenReturn(vm.spec);

        when(projectService.createProject(moveOutInfo.entitlementId.toString(), vps4User.getId(), moveInInfo.sgid))
                .thenReturn(project);

        when(virtualMachineService.insertVirtualMachine(any(InsertVirtualMachineParameters.class))).thenReturn(vm);

        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        resource = new PlatformMigrationResource(actionResource, user, vmResource, actionService, commandService,
                                                 config, creditService, imageService, networkService, projectService,
                                                 virtualMachineService, vmMoveImageMapService, vmMoveSpecMapService,
                                                 vmUserService, vps4UserService);
    }

    private void getMoveOutInfo() {
        moveOutInfo = new MoveOutInfo();
        moveOutInfo.vps4User = vps4User;
        moveOutInfo.entitlementId = vm.orionGuid;
        moveOutInfo.hostname = vm.hostname;
        moveOutInfo.hfsImageName = vm.image.hfsName;
        moveOutInfo.primaryIpAddress = vm.primaryIpAddress;
        moveOutInfo.serverName = vm.name;
        moveOutInfo.specName = vm.spec.name;
        moveOutInfo.vmUser = vmUser;
        moveOutInfo.actions = actions;
        moveOutInfo.additionalIps = additionalIps;
    }

    @Test
    public void moveOutCallsServices() {
        resource.moveOut(vmId);

        verify(virtualMachineService, atLeastOnce()).getVirtualMachine(vmId);
        verify(networkService, atLeastOnce()).getVmActiveSecondaryAddresses(vm.hfsVmId);
        verify(vmUserService, atLeastOnce()).getPrimaryCustomer(vmId);
        verify(vps4UserService, atLeastOnce()).getUser("test-vps4-shopper");
    }

    @Test
    public void moveOutCreatesMoveOutCommand() {
        resource.moveOut(vmId);
        verify(actionService, times(1)).createAction(eq(vmId), eq(ActionType.MOVE_OUT), anyString(), anyString());
    }

    @Test
    public void moveOutReturnsExpectedInfo() {
        MoveOutInfo result = resource.moveOut(vmId);

        assertEquals(vm.orionGuid, result.entitlementId);
        assertEquals(vm.name, result.serverName);
        assertEquals(vm.spec.specName, result.specName);
        assertEquals(vm.image.hfsName, result.hfsImageName);
        assertEquals(vm.hostname, result.hostname);
        assertEquals(vm.primaryIpAddress, result.primaryIpAddress);
        assertEquals(additionalIps, result.additionalIps);
        assertEquals(actions, result.actions);
        assertEquals(vmUser, result.vmUser);
        assertEquals(vps4User, result.vps4User);
    }

    @Test public void moveInInsertsUser() {
        resource.moveIn(moveInRequest);

        verify(vps4UserService, times(1)).getOrCreateUserForShopper(
                moveOutInfo.vps4User.getShopperId(),
                moveOutInfo.vps4User.getResellerId(),
                moveOutInfo.vps4User.getCustomerId());
    }

    @Test public void moveInCreatesProject() {
        resource.moveIn(moveInRequest);

        verify(projectService, times(1)).createProject(
                moveOutInfo.entitlementId.toString(),
                vps4User.getId(),
                moveInInfo.sgid);
    }

    @Test public void moveInInsertsVirtualMachine() {
        resource.moveIn(moveInRequest);

        verify(virtualMachineService, times(1))
                .insertVirtualMachine(insertVirtualMachineParametersCaptor.capture());
        InsertVirtualMachineParameters params = insertVirtualMachineParametersCaptor.getValue();
        assertEquals(moveInInfo.hfsVmId, params.hfsVmId);
        assertEquals(moveOutInfo.entitlementId, params.orionGuid);
        assertEquals(moveOutInfo.serverName, params.name);
        assertEquals(project.getProjectId(), params.projectId);
        assertEquals(vm.spec.specId, params.specId);
        assertEquals(vm.image.imageId, params.imageId);
        assertEquals(1, params.dataCenterId);
        assertEquals(moveOutInfo.hostname, params.hostname);
    }

    @Test public void moveInInsertsIpAddresses() {
        resource.moveIn(moveInRequest);

        verify(networkService, times(1))
                .createIpAddress(
                        0,
                        vm.vmId,
                        moveOutInfo.primaryIpAddress.ipAddress,
                        IpAddress.IpAddressType.PRIMARY);
        verify(networkService, times(additionalIps.size()))
                .createIpAddress(anyLong(), any(), anyString(), eq(IpAddress.IpAddressType.SECONDARY));
    }

    @Test public void moveInInsertsVmUser() {
        resource.moveIn(moveInRequest);

        verify(vmUserService, times(1)).createUser(moveOutInfo.vmUser.username, vm.vmId, moveOutInfo.vmUser.adminEnabled);
    }

    @Test public void moveInInsertsActions() {
        resource.moveIn(moveInRequest);

        verify(actionService, times(actions.size())).insertAction(eq(vm.vmId), any());
    }

    @Test
    public void moveInCreatesMoveInCommand() {
        resource.moveIn(moveInRequest);

        verify(actionService, times(1))
                .createAction(eq(vmId), eq(ActionType.MOVE_IN), anyString(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void moveInThrowsExceptionIfUnmappedSpec() {
        when(vmMoveSpecMapService.getVmMoveSpecMap(vm.spec.specId, ServerType.Platform.OPENSTACK))
                .thenThrow(new IllegalArgumentException("A mapping does not exist for this spec: 0"));
        resource.moveIn(moveInRequest);

        fail("Expected exception to be thrown.");
    }

    @Test(expected = Vps4Exception.class)
    public void moveInThrowsExceptionIfUnmappedImage() {
        when(vmMoveImageMapService.getVmMoveImageMap(vm.image.imageId, ServerType.Platform.OPENSTACK))
                .thenThrow(new IllegalArgumentException("A mapping does not exist for this image: 0"));
        when(imageService.getImageByHfsName(vm.image.hfsName)).thenReturn(vm.image);

        resource.moveIn(moveInRequest);

        fail("Expected exception to be thrown.");
    }

    @Test
    public void moveBackCreatesMoveBackCommand() {
        resource.moveBack(vmId);
        verify(actionService, times(1)).createAction(eq(vmId), eq(ActionType.MOVE_BACK), anyString(), anyString());
    }
}
