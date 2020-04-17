package com.godaddy.vps4.web.vm;

import java.util.List;

import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;

public class VirtualMachineWithDetails extends VirtualMachine {

    public VirtualMachineDetails virtualMachineDetails;
    public DataCenter dataCenter;
    public String shopperId;
    public AutomaticSnapshotSchedule autoSnapshots;
    public PanoptaServerDetails monitoringAgent;
    public List<ScheduledZombieCleanupJob> scheduledZombieCleanupJobs;

    public VirtualMachineWithDetails(VirtualMachine virtualMachine, VirtualMachineDetails virtualMachineDetails,
            DataCenter dataCenter, String shopperId, AutomaticSnapshotSchedule autoSnapshots,
            PanoptaServerDetails panoptaDetails, List<ScheduledZombieCleanupJob> scheduledZombieCleanupJobs) {
        super(virtualMachine);
        this.virtualMachineDetails = virtualMachineDetails;
        this.dataCenter = dataCenter;
        this.shopperId = shopperId;
        this.autoSnapshots = autoSnapshots;
        this.monitoringAgent = panoptaDetails;
        this.scheduledZombieCleanupJobs = scheduledZombieCleanupJobs;
    }
}
