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
import com.godaddy.vps4.util.Monitoring;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class HandleMonitoringDownEventTest {
    static Injector injector;

    private CommandContext context;
    VirtualMachine testVm = new VirtualMachine();
    VirtualMachineCredit credit = new VirtualMachineCredit();

    @Inject
    HandleMonitoringDownEvent command;
    @Inject
    MonitoringNotificationService monitoringNotificationService;
    @Inject
    CreditService creditService;
    @Inject
    Monitoring monitoring;

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                MonitoringNotificationService monitoringNotificationService = mock(MonitoringNotificationService.class);
                bind(MonitoringNotificationService.class).toInstance(monitoringNotificationService);

                Monitoring monitoring = mock(Monitoring.class);
                bind(Monitoring.class).toInstance(monitoring);

                CreditService creditService = mock(CreditService.class);
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
        when(monitoringNotificationService.sendServerDownEventNotification(testVm)).thenReturn(123L);
        when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);
        when(monitoring.hasFullyManagedMonitoring(credit)).thenReturn(true);

        command.execute(context, testVm);

        verify(monitoringNotificationService, times(1)).sendServerDownEventNotification(testVm);
    }

    @Test
    public void testHandleMonitoringDownEventAccountNotActive() {
        credit.accountStatus = AccountStatus.REMOVED;
        when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);
        when(monitoring.hasFullyManagedMonitoring(credit)).thenReturn(true);

        command.execute(context, testVm);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any(VirtualMachine.class));
    }

    @Test
    public void testHandleMonitoringDownEventAccountNotFullyManaged() {
        when(monitoring.hasFullyManagedMonitoring(credit)).thenReturn(false);
        when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        command.execute(context, testVm);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any(VirtualMachine.class));
    }
}