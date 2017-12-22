package com.godaddy.vps4.util;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Inject;

public class Monitoring {
        
    private final long accountId;
    private final static int FULLY_MANAGED_LEVEL = 2;
    private final static int MONITORING_LEVEL = 1;
    
    @Inject
    public Monitoring(Config config)
    {
        accountId = Long.parseLong(config.get("monitoring.nodeping.account.id"));
    }
    
    public Long getAccountId(VirtualMachine vm) {
        return getAccountId(vm.managedLevel); 
    }
    
    public Long getAccountId(int managedLevel) {
        return accountId; 
    }
    
    public boolean hasMonitoring(VirtualMachineCredit credit) {
        return credit.monitoring == MONITORING_LEVEL || hasFullyManagedMonitoring(credit);
    }

    public boolean hasFullyManagedMonitoring(VirtualMachineCredit credit) {
        return credit.managedLevel == FULLY_MANAGED_LEVEL;
    }
}
