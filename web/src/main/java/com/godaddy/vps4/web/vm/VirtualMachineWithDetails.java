package com.godaddy.vps4.web.vm;

import java.util.List;

import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.network.IpAddress;
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
    public String hypervisorHostname;
    public List<IpAddress> additionalIps;
    public boolean imported;

    public VirtualMachineWithDetails(VirtualMachine virtualMachine, VirtualMachineDetails virtualMachineDetails,
            DataCenter dataCenter, String shopperId, AutomaticSnapshotSchedule autoSnapshots,
            PanoptaServerDetails panoptaDetails, List<ScheduledZombieCleanupJob> scheduledZombieCleanupJobs,
            String hypervisorHostname, List<IpAddress> additionalIps, boolean imported) {
        super(virtualMachine);
        this.virtualMachineDetails = virtualMachineDetails;
        this.dataCenter = dataCenter;
        this.shopperId = shopperId;
        this.autoSnapshots = autoSnapshots;
        this.monitoringAgent = panoptaDetails;
        this.scheduledZombieCleanupJobs = scheduledZombieCleanupJobs;
        this.hypervisorHostname = hypervisorHostname;
        this.additionalIps = additionalIps;
        this.imported = imported;
    }
}
