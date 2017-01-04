package com.godaddy.vps4.web.vm;

import java.time.Instant;

import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineRequest;

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
    public Instant validOn;
    public Instant validUntil;
    public String controlPanel;

    public CombinedVm() {
    }

    public CombinedVm(Vm vm) {
        this(vm, null, null);
    }

    public CombinedVm(Vm vm, VirtualMachine virtualMachine, VirtualMachineRequest vmRequest) {
        if (vm != null) {
            vmId = vm.vmId;
            status = vm.status;
            running = vm.running;
            useable = vm.useable;
            if (vm.osinfo != null)
                image = vm.osinfo.name;
        }

        if (virtualMachine != null) {
            spec = virtualMachine.spec.name;
            name = virtualMachine.name;
            validOn = virtualMachine.validOn;
            validUntil = virtualMachine.validUntil;
        }

        if (vmRequest != null) {
            controlPanel = vmRequest.controlPanel;
        }
    }

    @Override
    public String toString() {
        return "Vm [vmId=" + vmId + ", status=" + status + "]";
    }
}
