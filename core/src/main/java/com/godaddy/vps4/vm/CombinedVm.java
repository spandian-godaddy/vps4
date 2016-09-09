package com.godaddy.vps4.vm;

import gdg.hfs.vhfs.vm.Vm;

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
            status = vm.status;
            running = vm.running;
            useable = vm.useable;
            if(vm.address != null)
                address = vm.address.ip_address;
            if(vm.osinfo != null)
                image = vm.osinfo.name;
        }

        if (virtualMachine != null) {
            vmId = virtualMachine.vmId;
            spec = virtualMachine.spec.name;
            name = virtualMachine.name;
        }
    }

    @Override
    public String toString() {
        return "Vm [vmId=" + vmId + ", status=" + status + "]";
    }
}
