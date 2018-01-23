package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;

public class VirtualMachineWithDetails extends VirtualMachine {

    public VirtualMachineDetails virtualMachineDetails;
    public DataCenter dataCenter;
    public SnapshotSchedule autoSnapshots;

    public VirtualMachineWithDetails(VirtualMachine virtualMachine, VirtualMachineDetails virtualMachineDetails, DataCenter dataCenter) {
        super(virtualMachine);
        this.virtualMachineDetails = virtualMachineDetails;
        this.dataCenter = dataCenter;
    }
    public VirtualMachineWithDetails(VirtualMachine virtualMachine, VirtualMachineDetails virtualMachineDetails,
                                     DataCenter dataCenter, SnapshotSchedule autoSnapshots) {
        super(virtualMachine);
        this.virtualMachineDetails = virtualMachineDetails;
        this.dataCenter = dataCenter;
        this.autoSnapshots = autoSnapshots;
    }
}
