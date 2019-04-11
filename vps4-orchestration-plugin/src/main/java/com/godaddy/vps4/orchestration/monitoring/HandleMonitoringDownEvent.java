package com.godaddy.vps4.orchestration.monitoring;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.monitoring.MonitoringNotificationService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class HandleMonitoringDownEvent implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(HandleMonitoringDownEvent.class);

    private final VirtualMachineService virtualMachineService;
    private final MonitoringNotificationService monitoringNotificationService;
    private final CreditService creditService;

    @Inject
    public HandleMonitoringDownEvent(VirtualMachineService virtualMachineService,
            MonitoringNotificationService monitoringNotificationService, CreditService creditService) {
        this.virtualMachineService = virtualMachineService;
        this.monitoringNotificationService = monitoringNotificationService;
        this.creditService = creditService;
    }

    @Override
    public Void execute(CommandContext context, Long nodePingCheckId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachineByCheckId(nodePingCheckId);
        if (vm == null) {
            logger.info("No VM found for alerting NodePing checkId: {}", nodePingCheckId);
        } else if (shouldSendNotification(vm)) {
            long irisIncidentId = monitoringNotificationService.sendServerDownEventNotification(vm);
            logger.info("VM-Down Event Notification sent for vmId {}, Iris Incident {} created", vm.vmId, irisIncidentId);
        } else {
            logger.info("VM-Down Event Ignored -  vmId {}", vm.vmId);
        }

        return null;
    }

    private boolean shouldSendNotification(VirtualMachine vm) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        boolean isFullyManaged = credit.isFullyManaged();
        boolean isAccountActive = credit.getAccountStatus() == AccountStatus.ACTIVE;
        //This is where I will check  any other conditions that would stop us from sending the notification.
        //those will be added in my next story.

        return isFullyManaged && isAccountActive;
    }
}
