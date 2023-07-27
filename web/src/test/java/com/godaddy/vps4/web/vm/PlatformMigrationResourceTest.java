package com.godaddy.vps4.web.vm;

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
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
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

    private GDUser gdUser;
    private UUID vmId = UUID.randomUUID();
    private VirtualMachine vm;
    private Project project = mock(Project.class);
    private List<IpAddress> additionalIps = new ArrayList<>();
    private List<Action> actions = new ArrayList<>();
    private PanoptaDetail panoptaDetail = mock(PanoptaDetail.class);
    private VmUser vmUser = mock(VmUser.class);
    private Vps4User vps4User = mock(Vps4User.class);
    private Action moveOutAction = mock(Action.class);

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

        moveOutAction.commandId = UUID.randomUUID();;

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
    }

    @Test
    public void moveOutCallsServices() {
        getPlatformMigrationResource().moveOut(vmId);

        verify(virtualMachineService, atLeastOnce()).getVirtualMachine(vmId);
        verify(projectService, atLeastOnce()).getProject(vm.projectId);
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
        assertEquals(vm.spec, result.spec);
        assertEquals(vm.image, result.image);
        assertEquals(vm.hostname, result.hostname);
        assertEquals(project, result.project);
        assertEquals(vm.primaryIpAddress, result.primaryIpAddress);
        assertEquals(additionalIps, result.additionalIps);
        assertEquals(actions, result.actions);
        assertEquals(panoptaDetail, result.panoptaDetail);
        assertEquals(vmUser, result.vmUser);
        assertEquals(vps4User, result.vps4User);
        assertEquals(moveOutAction.commandId, result.commandId);
    }
}
