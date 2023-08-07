package com.godaddy.vps4.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.credit.CreditHistory;

public interface VirtualMachineService {

    VirtualMachine getVirtualMachine(long hfsVmId);

    VirtualMachine getVirtualMachine(UUID vmId);

    List<CreditHistory> getCreditHistory(UUID orionGuid);

    ServerSpec getSpec(String name);

    ServerSpec getSpec(int tier, int serverTypeId);

    ServerSpec getSpec(int specId);

    Map<Integer, Integer> getActiveServerCountByTiers();

    Map<Integer, Integer> getZombieServerCountByTiers();

    void setVmRemoved(UUID vmId);

    VirtualMachine provisionVirtualMachine(ProvisionVirtualMachineParameters vmProvisionParameters);

    VirtualMachine importVirtualMachine(InsertVirtualMachineParameters insertVirtualMachineParameters);

    VirtualMachine insertVirtualMachine(InsertVirtualMachineParameters insertVirtualMachineParameters);

    void addHfsVmIdToVirtualMachine(UUID vmId, long hfsVmId);

    void setHostname(UUID vmId, String hostname);

    void setBackupJobId(UUID vmId, UUID backupJobId);

    void updateVirtualMachine(UUID vmId, Map<String, Object> paramsToUpdate);

    boolean virtualMachineHasCpanel(UUID vmId);

    boolean virtualMachineHasPlesk(UUID vmId);

    long getUserIdByVmId(UUID vmId);

    UUID getOrionGuidByVmId(UUID vmId);

    long getHfsVmIdByVmId(UUID vmId);

    Long getPendingSnapshotActionIdByVmId(UUID vmId);

    String getOSDistro(UUID vmId);

    boolean isLinux(UUID vmId);

    boolean hasControlPanel(UUID vmId);

    void setVmCanceled(UUID vmId);

    void reviveZombieVm(UUID vmId, UUID newOrionGuid);

	List<VirtualMachine> getVirtualMachines(VirtualMachineType type, Long vps4UserId, String ipAddress, UUID orionGuid,
                                            Long hfsVmId, Integer dcId, String platform);

    UUID getImportedVm(UUID vmId);

    void ackNydusWarning(UUID vmId);

    void setMonitoringPlanFeature(UUID vmId, boolean monitoring);

    boolean getMonitoringPlanFeature (UUID orionGuid);
}
