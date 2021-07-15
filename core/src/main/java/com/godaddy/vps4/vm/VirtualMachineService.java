package com.godaddy.vps4.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.credit.CreditHistory;

public interface VirtualMachineService {

    List<VirtualMachine> getVirtualMachinesForProject(long projectId);

    VirtualMachine getVirtualMachine(long hfsVmId);

    VirtualMachine getVirtualMachine(UUID vmId);

    VirtualMachine getVirtualMachineByCheckId(long nodePingcheckId);

    List<CreditHistory> getCreditHistory(UUID orionGuid);

    ServerSpec getSpec(String name);

    ServerSpec getSpec(int tier, int serverTypeId);

    Map<Integer, Integer> getActiveServerCountByTiers();

    Map<Integer, Integer> getZombieServerCountByTiers();

    /**
     * @param vmId - the VM ID from the VM Vertical Service
     * @param projectId
     * @param spec
     * @param name
     * @return
     */
//    VirtualMachine createVirtualMachine(long vmId, long projectId, String spec, String name);

    // updateStatus(long vmId, int newStatus)

    void setVmRemoved(UUID vmId);

    VirtualMachine provisionVirtualMachine(ProvisionVirtualMachineParameters vmProvisionParameters);

    public class ImportVirtualMachineParameters {
        public ImportVirtualMachineParameters(long hfsVmId, UUID orionGuid, String name, long projectId, int tier, long imageId) {
            this.hfsVmId = hfsVmId;
            this.orionGuid = orionGuid;
            this.name = name;
            this.projectId = projectId;
            this.specId = tier;
            this.imageId = imageId;
        }

        public long hfsVmId;
        public UUID orionGuid;
        public String name;
        public long projectId;
        public int specId;
        public long imageId;
    }

    VirtualMachine importVirtualMachine(ImportVirtualMachineParameters importVirtualMachineParameters);

    public class ProvisionVirtualMachineParameters {
        public ProvisionVirtualMachineParameters(long vps4UserId, int dataCenterId, String sgidPrefix, UUID orionGuid, String name, int tier,
                int managedLevel, String image) {
            this.vps4UserId = vps4UserId;
            this.dataCenterId = dataCenterId;
            this.sgidPrefix = sgidPrefix;
            this.orionGuid = orionGuid;
            this.name = name;
            this.tier = tier;
            this.managedLevel = managedLevel;
            this.imageHfsName = image;

        }

        private long vps4UserId;
        private int dataCenterId;
        private String sgidPrefix;
        private UUID orionGuid;
        private String name;
        private int tier;
        private int managedLevel;
        private String imageHfsName;

        public long getVps4UserId() {
            return vps4UserId;
        }

        public int getDataCenterId() {
            return dataCenterId;
        }

        public String getSgidPrefix() {
            return sgidPrefix;
        }

        public UUID getOrionGuid() {
            return orionGuid;
        }

        public String getName() {
            return name;
        }

        public int getTier() {
            return tier;
        }

        public int getManagedLevel() {
            return managedLevel;
        }

        public String getImageHfsName() {
            return imageHfsName;
        }
    }

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

    void setVmZombie(UUID vmId);

    void reviveZombieVm(UUID vmId, UUID newOrionGuid);

	List<VirtualMachine> getVirtualMachines(VirtualMachineType type, Long vps4UserId, String ipAddress, UUID orionGuid,
			Long hfsVmId);

    UUID getImportedVm(UUID vmId);

    void ackNydusWarning(UUID vmId);

    void setMonitoringPlanFeature(UUID vmId, boolean monitoring);

    boolean getMonitoringPlanFeature (UUID orionGuid);
}
