package com.godaddy.vps4.orchestration.monitoring;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import javax.inject.Inject;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.monitoring.MonitoringNotificationService;
import com.godaddy.vps4.util.Monitoring;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;

public class HandleMonitoringDownEvent implements Command<VirtualMachine, Void> {

    private final MonitoringNotificationService monitoringNotificationService;
    private final Monitoring monitoring;
    private final CreditService creditService;

    @Inject
    public HandleMonitoringDownEvent(MonitoringNotificationService monitoringNotificationService, Monitoring monitoring,
            CreditService creditService) {
        this.monitoringNotificationService = monitoringNotificationService;
        this.monitoring = monitoring;
        this.creditService = creditService;
    }

    @Override
    public Void execute(CommandContext context, VirtualMachine vm) {
        if (shouldSendNotification(vm)) {
            monitoringNotificationService.sendServerDownEventNotification(vm);
        }

        return null;
    }

    private boolean shouldSendNotification(VirtualMachine vm) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        boolean isFullyManaged = monitoring.hasFullyManagedMonitoring(credit);
        boolean isAccountActive = credit.accountStatus == AccountStatus.ACTIVE;
        //This is where I will check  any other conditions that would stop us from sending the notification.
        //those will be added in my next story.

        return isFullyManaged && isAccountActive;
    }
}
