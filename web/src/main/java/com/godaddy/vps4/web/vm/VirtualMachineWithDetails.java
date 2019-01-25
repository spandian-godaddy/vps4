package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;

public class VirtualMachineWithDetails extends VirtualMachine {

    public VirtualMachineDetails virtualMachineDetails;
    public DataCenter dataCenter;
    public String shopperId;
    public AutomaticSnapshotSchedule autoSnapshots;

    public VirtualMachineWithDetails(VirtualMachine virtualMachine, VirtualMachineDetails virtualMachineDetails,
                                     DataCenter dataCenter, String shopperId, AutomaticSnapshotSchedule autoSnapshots) {
        super(virtualMachine);
        this.virtualMachineDetails = virtualMachineDetails;
        this.dataCenter = dataCenter;
        this.shopperId = shopperId;
        this.autoSnapshots = autoSnapshots;
    }
}
