package com.godaddy.vps4.monitoring;

import com.godaddy.vps4.vm.VirtualMachine;

public interface MonitoringNotificationService {

    public long sendServerDownEventNotification(VirtualMachine vm);

}
