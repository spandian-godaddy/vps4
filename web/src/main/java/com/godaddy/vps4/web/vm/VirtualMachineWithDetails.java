package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.VirtualMachine;

public class VirtualMachineWithDetails extends VirtualMachine {

    public VirtualMachineDetails virtualMachineDetails;

    public VirtualMachineWithDetails(VirtualMachine virtualMachine, VirtualMachineDetails virtualMachineDetails) {
        super(virtualMachine);
        this.virtualMachineDetails = virtualMachineDetails;
    }
}
