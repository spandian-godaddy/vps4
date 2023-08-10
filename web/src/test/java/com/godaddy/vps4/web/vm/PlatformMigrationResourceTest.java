package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.move.VmMoveImageMap;
import com.godaddy.vps4.move.VmMoveImageMapService;
import com.godaddy.vps4.move.VmMoveSpecMap;
import com.godaddy.vps4.move.VmMoveSpecMapService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.InsertVirtualMachineParameters;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
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

public class PlatformMigrationResourceTest {
    private final ActionService actionService = mock(ActionService.class);
    private final CommandService commandService = mock(CommandService.class);
    private final VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final NetworkService networkService = mock(NetworkService.class);
    private final ActionResource actionResource = mock(ActionResource.class);
    private final PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private final VmUserService vmUserService = mock(VmUserService.class);
    private final Vps4UserService vps4UserService = mock(Vps4UserService.class);
    private final Config config = mock(Config.class);
    private final CreditService creditService = mock(CreditService.class);
    private final VmMoveImageMapService vmMoveImageMapService = mock(VmMoveImageMapService.class);
    private final VmMoveSpecMapService vmMoveSpecMapService = mock(VmMoveSpecMapService.class);
    private final ImageService imageService = mock(ImageService.class);

    private GDUser gdUser;
    private UUID vmId = UUID.randomUUID();
    private VirtualMachine vm;
    private Project project;
    private List<IpAddress> additionalIps = new ArrayList<>();
    private List<Action> actions = new ArrayList<>();
    private PanoptaDetail panoptaDetail = mock(PanoptaDetail.class);
    private VmUser vmUser = mock(VmUser.class);
    private Vps4User vps4User;
    private Action moveOutAction = mock(Action.class);
    private MoveOutInfo moveOutInfo;
    private MoveInInfo moveInInfo;
    private MoveInRequest moveInRequest;
    @Captor private ArgumentCaptor<InsertVirtualMachineParameters> insertVirtualMachineParametersCaptor;

    private final Injector injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                public void configure() {
                    bind(ActionService.class).toInstance(actionService);
                    bind(CommandService.class).toInstance(commandService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                    bind(ProjectService.class).toInstance(projectService);
                    bind(NetworkService.class).toInstance(networkService);
                    bind(ActionResource.class).toInstance(actionResource);
                    bind(PanoptaDataService.class).toInstance(panoptaDataService);
                    bind(VmUserService.class).toInstance(vmUserService);
                    bind(Vps4UserService.class).toInstance(vps4UserService);
                    bind(Config.class).toInstance(config);
                    bind(CreditService.class).toInstance(creditService);
                    bind(VmMoveImageMapService.class).toInstance(vmMoveImageMapService);
                    bind(VmMoveSpecMapService.class).toInstance(vmMoveSpecMapService);
                    bind(ImageService.class).toInstance(imageService);
                }

                @Provides
                public GDUser provideUser() {
                    return gdUser;
                }
            });

    private PlatformMigrationResource getPlatformMigrationResource() {
        return injector.getInstance(PlatformMigrationResource.class);
    }

    @Before
    public void setup() {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);

        vm = new VirtualMachine();
        vm.vmId = vmId;
        vm.orionGuid = UUID.randomUUID();
        vm.name = "mock-vm";
        vm.spec = mock(ServerSpec.class);
        vm.image = mock(Image.class);
        vm.hostname = "fake-hostname";
        vm.projectId = 12345L;
        vm.primaryIpAddress = mock(IpAddress.class);
        vm.hfsVmId = 67890L;

        gdUser = GDUserMock.createShopper();
        vps4User = new Vps4User(12, gdUser.getShopperId(), UUID.randomUUID(), "1");
        project = new Project(vm.projectId, "testProject", "testSgid", Instant.now(), Instant.MAX, vps4User.getId());

        getMoveOutInfo();
        moveInInfo = new MoveInInfo();
        moveInInfo.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        moveInInfo.sgid = "testMoveIn";
        moveInInfo.hfsVmId = 132;

        moveOutAction.commandId = UUID.randomUUID();;

        moveInRequest = new MoveInRequest();
        moveInRequest.moveOutInfo = moveOutInfo;
        moveInRequest.moveInInfo = moveInInfo;

        when(actionService.getAction(anyLong())).thenReturn(moveOutAction);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        when(projectService.getProject(vm.projectId)).thenReturn(project);
        when(networkService.getVmSecondaryAddress(vm.hfsVmId)).thenReturn(additionalIps);
        when(actionResource.getActionList(null, null, null, null, null,
                null, Long.MAX_VALUE, 0)).thenReturn(actions);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        when(vmUserService.getPrimaryCustomer(vmId)).thenReturn(vmUser);
        when(vps4UserService.getUser(gdUser.getShopperId())).thenReturn(vps4User);
        when(config.get("imported.datacenter.defaultId")).thenReturn("1");
        when(vps4UserService.getOrCreateUserForShopper(
                moveOutInfo.vps4User.getShopperId(),
                moveOutInfo.vps4User.getResellerId(),
                moveOutInfo.vps4User.getCustomerId()))
                .thenReturn(vps4User);

        VmMoveImageMap vmMoveImageMap = new VmMoveImageMap();
        vmMoveImageMap.fromImageId = vm.image.imageId;
        vmMoveImageMap.toImageId = vm.image.imageId + 10;
        vmMoveImageMap.id = 1;
        when(vmMoveImageMapService.getVmMoveImageMap(vm.image.imageId, ServerType.Platform.OPTIMIZED_HOSTING))
                .thenReturn(vmMoveImageMap);
        when(imageService.getImageByHfsName(vm.image.hfsName)).thenReturn(vm.image);
        when(imageService.getImage(vmMoveImageMap.toImageId)).thenReturn(vm.image);

        VmMoveSpecMap vmMoveSpecMap = new VmMoveSpecMap();
        vmMoveSpecMap.fromSpecId = vm.spec.specId;
        vmMoveSpecMap.toSpecId = vm.spec.specId + 5;
        vmMoveSpecMap.id = 1;
        when(vmMoveSpecMapService.getVmMoveSpecMap(vm.spec.specId, ServerType.Platform.OPTIMIZED_HOSTING))
                .thenReturn(vmMoveSpecMap);
        when(virtualMachineService.getSpec(vm.spec.specName)).thenReturn(vm.spec);
        when(virtualMachineService.getSpec(vmMoveSpecMap.toSpecId)).thenReturn(vm.spec);

        when(projectService.createProject(moveOutInfo.entitlementId.toString(), vps4User.getId(), moveInInfo.sgid))
                .thenReturn(project);

        when(virtualMachineService.insertVirtualMachine(any(InsertVirtualMachineParameters.class))).thenReturn(vm);
    }

    private void getMoveOutInfo() {
        moveOutInfo = new MoveOutInfo();
        moveOutInfo.panoptaDetail = panoptaDetail;
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
        getPlatformMigrationResource().moveOut(vmId);

        verify(virtualMachineService, atLeastOnce()).getVirtualMachine(vmId);
        verify(networkService, atLeastOnce()).getVmSecondaryAddress(vm.hfsVmId);
        verify(panoptaDataService, atLeastOnce()).getPanoptaDetails(vmId);
        verify(vmUserService, atLeastOnce()).getPrimaryCustomer(vmId);
        verify(vps4UserService, atLeastOnce()).getUser(gdUser.getShopperId());
    }

    @Test
    public void moveOutCallsInterventionEndpoint() {
        // TODO: Implement
    }

    @Test
    public void moveOutCreatesMoveOutCommand() {
        getPlatformMigrationResource().moveOut(vmId);
        verify(actionService, times(1)).createAction(eq(vmId), eq(ActionType.MOVE_OUT), anyString(), anyString());
    }

    @Test
    public void moveOutReturnsExpectedInfo() {
        MoveOutInfo result = getPlatformMigrationResource().moveOut(vmId);

        assertEquals(vm.orionGuid, result.entitlementId);
        assertEquals(vm.name, result.serverName);
        assertEquals(vm.spec.specName, result.specName);
        assertEquals(vm.image.hfsName, result.hfsImageName);
        assertEquals(vm.hostname, result.hostname);
        assertEquals(vm.primaryIpAddress, result.primaryIpAddress);
        assertEquals(additionalIps, result.additionalIps);
        assertEquals(actions, result.actions);
        assertEquals(panoptaDetail, result.panoptaDetail);
        assertEquals(vmUser, result.vmUser);
        assertEquals(vps4User, result.vps4User);
    }

    @Test public void moveInInsertsUser() {
        getPlatformMigrationResource().moveIn(moveInRequest);

        verify(vps4UserService, times(1)).getOrCreateUserForShopper(
                moveOutInfo.vps4User.getShopperId(),
                moveOutInfo.vps4User.getResellerId(),
                moveOutInfo.vps4User.getCustomerId());
    }

    @Test public void moveInCreatesProject() {
        getPlatformMigrationResource().moveIn(moveInRequest);

        verify(projectService, times(1)).createProject(
                moveOutInfo.entitlementId.toString(),
                vps4User.getId(),
                moveInInfo.sgid);
    }

    @Test public void moveInInsertsVirtualMachine() {
        getPlatformMigrationResource().moveIn(moveInRequest);

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
        getPlatformMigrationResource().moveIn(moveInRequest);

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
        getPlatformMigrationResource().moveIn(moveInRequest);

        verify(vmUserService, times(1)).createUser(moveOutInfo.vmUser.username, vm.vmId);
    }

    @Test public void moveInInsertsActions() {
        getPlatformMigrationResource().moveIn(moveInRequest);

        verify(actionService, times(actions.size())).insertAction(eq(vm.vmId), any());
    }

    @Test public void moveInInsertsPanoptaRecords() {
        getPlatformMigrationResource().moveIn(moveInRequest);

        verify(panoptaDataService, times(1))
                .createOrUpdatePanoptaCustomer(
                        moveOutInfo.panoptaDetail.getPartnerCustomerKey(),
                        moveOutInfo.panoptaDetail.getCustomerKey());
        verify(panoptaDataService, times(1))
                .insertPanoptaServer(
                        vm.vmId,
                        moveOutInfo.panoptaDetail.getPartnerCustomerKey(),
                        moveOutInfo.panoptaDetail.getServerId(),
                        moveOutInfo.panoptaDetail.getServerKey(),
                        moveOutInfo.panoptaDetail.getTemplateId());
    }

    @Test
    public void moveInCreatesMoveInCommand() {
        getPlatformMigrationResource().moveIn(moveInRequest);

        verify(actionService, times(1))
                .createAction(eq(vmId), eq(ActionType.MOVE_IN), anyString(), anyString());
    }

    @Test
    public void moveInEndsIntervention() {
        // TODO: Implement
    }
}
