package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ControlPanelService;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerType.Platform;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;

public class DedicatedDestroyTest {
    private GDUser user;
    private CommandService commandService = mock(CommandService.class);
    private CommandState commandState = mock(CommandState.class);
    private ActionService actionService = mock(ActionService.class);
    private CreditService creditService = mock(CreditService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private TroubleshootVmService troubleshootVmService = mock(TroubleshootVmService.class);

    private VmResource vmResource;
    private VirtualMachine vm;

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;


    @Captor private ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new SnapshotModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CommandService.class).toInstance(commandService);
                    bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
                    bind(CreditService.class).toInstance(creditService);
                    bind(ActionService.class).toInstance(actionService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                    bind(TroubleshootVmService.class).toInstance(troubleshootVmService);

                    MapBinder<ActionType, String> actionTypeToCancelCmdNameMapBinder
                            = MapBinder.newMapBinder(binder(), ActionType.class, String.class);
                    actionTypeToCancelCmdNameMapBinder.addBinding(ActionType.CREATE_SNAPSHOT)
                            .toInstance("CreateSnapshotCancelCommand");

                    bind(SchedulerWebService.class).toInstance(mock(SchedulerWebService.class));
                    bind(VmService.class).toInstance(mock(VmService.class));
                    bind(ControlPanelService.class).toInstance(mock(ControlPanelService.class));
                    bind(ImageService.class).toInstance(mock(ImageService.class));
                    bind(VmUserService.class).toInstance(mock(VmUserService.class));
                    bind(NetworkService.class).toInstance(mock(NetworkService.class));
                    bind(DataCenterService.class).toInstance(mock(DataCenterService.class));
                    bind(ScheduledJobService.class).toInstance(mock(ScheduledJobService.class));
                    bind(MonitorService.class).toInstance(mock(MonitorService.class));
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));

                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        commandState.commandId = UUID.randomUUID();
        vm = new VirtualMachine();
        vm.validUntil = Instant.MAX;

        ServerSpec spec = new ServerSpec();
        spec.serverType = new ServerType();
        spec.serverType.serverType = ServerType.Type.DEDICATED;
        spec.serverType.platform = Platform.OVH;
        vm.spec = spec;
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(vm);

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
            .withAccountStatus(AccountStatus.ACTIVE)
            .withShopperID("validUserShopperId")
            .build();
        when(creditService.getVirtualMachineCredit(any(UUID.class))).thenReturn(credit);

        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
        vmResource = injector.getInstance(VmResource.class);

        Action a = new Action(123L, vm.vmId, ActionType.DESTROY_VM, null, null, null,
                ActionStatus.NEW, Instant.now(), Instant.now(), null, UUID.randomUUID(), "validUserShopperId");

        when(actionService.createAction(eq(vm.vmId), eq(ActionType.DESTROY_VM), any(String.class), any(String.class))).thenReturn(a.id);
        when(actionService.getAction(a.id)).thenReturn(a);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    @Test
    public void testDestroyDedicated() {
        vmResource.destroyVm(UUID.randomUUID());
        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Assert.assertEquals("Vps4DestroyDedicated", commandSpec.command);

    }

    @Test
    public void testDestroyVM() {
        vm.spec.serverType.serverType = ServerType.Type.VIRTUAL;
        vm.spec.serverType.platform = Platform.OPENSTACK;

        vmResource.destroyVm(UUID.randomUUID());
        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Assert.assertEquals("Vps4DestroyVm", commandSpec.command);

    }

    @Test
    public void testDestroyOptimizedHostingVM() {
        vm.spec.serverType.serverType = ServerType.Type.VIRTUAL;
        vm.spec.serverType.platform = Platform.OPTIMIZED_HOSTING;

        vmResource.destroyVm(UUID.randomUUID());
        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Assert.assertEquals("Vps4DestroyOHVm", commandSpec.command);

    }
}
