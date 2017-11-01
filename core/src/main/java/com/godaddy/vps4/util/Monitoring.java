package com.godaddy.vps4.util;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Inject;

public class Monitoring {
        
    private final long accountId;
    private final long fullyManagedAccountId;
    private final static int FULLY_MANAGED_LEVEL = 2;
    
    @Inject
    public Monitoring(Config config)
    {
        accountId = Long.parseLong(config.get("nodeping.accountid"));
        fullyManagedAccountId = Long.parseLong(config.get("nodeping.fullyManaged.accountid"));
    }
    
    public Long getAccountId(VirtualMachine vm) {
        return getAccountId(vm.managedLevel); 
    }
    
    public Long getAccountId(int managedLevel) {
        return managedLevel == FULLY_MANAGED_LEVEL ? fullyManagedAccountId : accountId; 
    }
}
