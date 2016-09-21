package com.godaddy.vps4.vm;

import com.godaddy.vps4.hfs.Vm;

public class CombinedVm {
    public long vmId;
    public String status;
    public boolean running;
    public boolean useable;
    public String address;
    public String spec;
    public String image;
    public String name;

    public CombinedVm(){}
    
    public CombinedVm(Vm vm) {
        this(vm, null);
    }

    public CombinedVm(Vm vm, VirtualMachine virtualMachine) {
        if(vm != null) {
        	vmId = vm.vmId;
            status = vm.status;
            running = vm.running;
            useable = vm.useable;
            if(vm.address != null)
                address = vm.address.ip_address;
            if(vm.osinfo != null)
                image = vm.osinfo.name;
        }

        if (virtualMachine != null) {
            spec = virtualMachine.spec.name;
            name = virtualMachine.name;
        }
    }

    @Override
    public String toString() {
        return "Vm [vmId=" + vmId + ", status=" + status + "]";
    }
}
