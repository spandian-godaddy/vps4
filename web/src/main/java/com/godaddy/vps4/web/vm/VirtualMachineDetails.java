package com.godaddy.vps4.web.vm;

import gdg.hfs.vhfs.vm.Vm;

public class VirtualMachineDetails {

    public Long vmId;
    public String status;
    public boolean running;
    public boolean useable;
    
    public VirtualMachineDetails (Vm hfsVm) {
        if (hfsVm == null) {
            vmId = null;
            status = "REQUESTING";
            running = false;
            useable = false;
        }
        else {
            vmId = hfsVm.vmId;
            status = hfsVm.status;
            running = hfsVm.running;
            useable = hfsVm.useable;
        }
    }

    @Override
    public String toString() {
        return "Vm [vmId=" + vmId + ", status=" + status + ", running=" + running + ", useable=" + useable + "]";
    }

}
