package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.monitoring.MonitoringNotificationService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class HandleMonitoringDownEventTest {
    static Injector injector;

    private CommandContext context;
    Long dummyCheckId = 123L;
    Long dummyIrisId = 456L;
    VirtualMachine testVm = new VirtualMachine();
    VirtualMachineCredit credit = new VirtualMachineCredit();

    @Inject HandleMonitoringDownEvent command;

    private VirtualMachineService virtualMachineService;
    private MonitoringNotificationService monitoringNotificationService;
    private CreditService creditService;

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                virtualMachineService = mock(VirtualMachineService.class);
                bind(VirtualMachineService.class).toInstance(virtualMachineService);

                monitoringNotificationService = mock(MonitoringNotificationService.class);
                bind(MonitoringNotificationService.class).toInstance(monitoringNotificationService);

                creditService = mock(CreditService.class);
                bind(CreditService.class).toInstance(creditService);
            }
        });

        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        context = mock(CommandContext.class);

        testVm.orionGuid = UUID.randomUUID();

        credit.accountStatus = AccountStatus.ACTIVE;
        credit.managedLevel = 2;
        credit.orionGuid = testVm.orionGuid;
    }

    @Test
    public void testHandleMonitoringDownEvent() {
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(testVm);
        when(monitoringNotificationService.sendServerDownEventNotification(testVm)).thenReturn(dummyIrisId);
        when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, times(1)).sendServerDownEventNotification(testVm);
    }

    @Test
    public void testHandleMonitoringDownEventNoCheckIdFound() {
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(null);
        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any());
    }

    @Test
    public void testHandleMonitoringDownEventAccountNotActive() {
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(testVm);
        credit.accountStatus = AccountStatus.REMOVED;
        when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any(VirtualMachine.class));
    }

    @Test
    public void testHandleMonitoringDownEventAccountNotFullyManaged() {
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(testVm);
        credit.managedLevel = 0;
        when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any(VirtualMachine.class));
    }
}