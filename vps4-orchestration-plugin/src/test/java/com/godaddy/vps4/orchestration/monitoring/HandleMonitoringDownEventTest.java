package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.DataCenterService;
import gdg.hfs.vhfs.ecomm.Account;
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
    VirtualMachineCredit credit;

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
    }

    private void setupCredit(int managedLevel, AccountStatus accountStatus) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("managed_level", String.valueOf(managedLevel));
        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountStatus(Account.Status.valueOf(accountStatus.toString().toLowerCase()))
                .withAccountGuid(testVm.orionGuid.toString())
                .withPlanFeatures(planFeatures)
                .build();
    }

    @Test
    public void testHandleMonitoringDownEvent() {
        setupCredit(2, AccountStatus.ACTIVE);
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(testVm);
        when(monitoringNotificationService.sendServerDownEventNotification(testVm)).thenReturn(dummyIrisId);
        when(creditService.getVirtualMachineCredit(credit.getOrionGuid())).thenReturn(credit);

        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, times(1)).sendServerDownEventNotification(testVm);
    }

    @Test
    public void testHandleMonitoringDownEventNoCheckIdFound() {
        setupCredit(2, AccountStatus.ACTIVE);
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(null);
        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any());
    }

    @Test
    public void testHandleMonitoringDownEventAccountNotActive() {
        setupCredit(2, AccountStatus.REMOVED);
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(testVm);
        when(creditService.getVirtualMachineCredit(credit.getOrionGuid())).thenReturn(credit);

        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any(VirtualMachine.class));
    }

    @Test
    public void testHandleMonitoringDownEventAccountNotFullyManaged() {
        setupCredit(0, AccountStatus.ACTIVE);
        when(virtualMachineService.getVirtualMachineByCheckId(dummyCheckId)).thenReturn(testVm);
        when(creditService.getVirtualMachineCredit(credit.getOrionGuid())).thenReturn(credit);

        command.execute(context, dummyCheckId);

        verify(monitoringNotificationService, never()).sendServerDownEventNotification(any(VirtualMachine.class));
    }
}