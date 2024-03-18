package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

public class VmSyncStatusResourceTest {

    VmSyncStatusResource vmSyncStatusResource;
    VmResource vmResource = mock(VmResource.class);
    private CommandState commandState = mock(CommandState.class);
    CommandService commandService = mock(CommandService.class);
    ActionService actionService = mock(ActionService.class);
    Action action = mock(Action.class);
    private long actionId = 1231231123;
    VirtualMachine testVm;

    private GDUser user;

    @Captor private ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor;

    private Injector injector = Guice.createInjector(
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(ActionService.class).toInstance(actionService);
                    bind(VmResource.class).toInstance(vmResource);
                    bind(Action.class).toInstance(action);
                    bind(CommandService.class).toInstance(commandService);
                }

            });

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        vmSyncStatusResource = getVmSyncStatusResource();

        user = GDUserMock.createShopper();
        testVm = new VirtualMachine();
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = UUID.randomUUID();
        testVm.canceled = Instant.now().plus(7, ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;

        commandState.commandId = UUID.randomUUID();

        when(vmResource.getVm(testVm.vmId)).thenReturn(testVm);

        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
        vmResource = injector.getInstance(VmResource.class);

        Action a = new Action(actionId, testVm.vmId, ActionType.SYNC_STATUS, null, null, null,
                ActionStatus.NEW, Instant.now(), Instant.now(), null, UUID.randomUUID(), "validUserShopperId");

        when(actionService.createAction(eq(testVm.vmId), eq(ActionType.SYNC_STATUS), any(String.class), any(String.class))).thenReturn(a.id);
        when(actionService.getAction(a.id)).thenReturn(a);

        Action b = new Action(actionId, testVm.vmId, ActionType.SYNC_OS, null, null, null,
                ActionStatus.NEW, Instant.now(), Instant.now(), null, UUID.randomUUID(), "validUserShopperId");

        when(actionService.createAction(eq(testVm.vmId), eq(ActionType.SYNC_OS), any(String.class), any(String.class))).thenReturn(b.id);
        when(actionService.getAction(b.id)).thenReturn(b);
    }

    private VmSyncStatusResource getVmSyncStatusResource() {
        return injector.getInstance(VmSyncStatusResource.class);
    }

    @Test
    public void testSyncStatusCreatesCommand() {
        getVmSyncStatusResource().sync(testVm.vmId, VmSyncStatusResource.SyncType.STATUS);

        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Assert.assertEquals("Vps4SyncVmStatus", commandSpec.command);

    }

    @Test
    public void testSyncStatusCreatesAction() {
        getVmSyncStatusResource().sync(testVm.vmId, VmSyncStatusResource.SyncType.STATUS);

        verify(actionService, times(1)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.SYNC_STATUS), anyObject(), anyString());
    }

    @Test
    public void syncStatusActionIsReturned() {
        VmAction actualReturnValue =
                getVmSyncStatusResource().sync(testVm.vmId, VmSyncStatusResource.SyncType.STATUS);

        assertEquals(actionId, actualReturnValue.id);
    }

    @Test
    public void testSyncOsInfoCreatesCommand() {
        getVmSyncStatusResource().sync(testVm.vmId, VmSyncStatusResource.SyncType.OS_INFO);

        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Assert.assertEquals("Vps4SyncOsStatus", commandSpec.command);
    }

    @Test
    public void testSyncOsInfoCreatesAction() {
        getVmSyncStatusResource().sync(testVm.vmId, VmSyncStatusResource.SyncType.OS_INFO);

        verify(actionService, times(1)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.SYNC_OS), anyObject(), anyString());
    }

    @Test
    public void syncOsInfoActionIsReturned() {
        VmAction actualReturnValue =
                getVmSyncStatusResource().sync(testVm.vmId, VmSyncStatusResource.SyncType.OS_INFO);

        assertEquals(actionId, actualReturnValue.id);
    }
}
