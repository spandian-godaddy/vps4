package com.godaddy.vps4.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.security.Vps4User;

public interface VirtualMachineService {

    List<VirtualMachine> getVirtualMachinesForProject(long projectId);

    VirtualMachine getVirtualMachine(long hfsVmId);

    VirtualMachine getVirtualMachineByOrionGuid(UUID orionGuid);

    VirtualMachine getVirtualMachine(UUID vmId);

    VirtualMachineSpec getSpec(String name);

    VirtualMachineSpec getSpec(int tier);

    /**
     * @param vmId - the VM ID from the VM Vertical Service
     * @param projectId
     * @param spec
     * @param name
     * @return
     */
//    VirtualMachine createVirtualMachine(long vmId, long projectId, String spec, String name);

    // updateStatus(long vmId, int newStatus)

    void destroyVirtualMachine(long vmId); // (just updates status/sets validUntil, destroy is accomplished on backend)

    void createVirtualMachineCredit(UUID orionGuid, String osType, String controlPanel, int tier, int managedLevel, String shopperId);

    VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid);

    VirtualMachine provisionVirtualMachine(ProvisionVirtualMachineParameters vmProvisionParameters);

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
    
    void updateVirtualMachine(UUID vmId, Map<String, Object> paramsToUpdate);

    List<VirtualMachine> getVirtualMachinesForUser(long vps4UserId);

    List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId);

    void createCreditIfNoneExists(Vps4User vps4User);
    
    boolean virtualMachineHasCpanel(UUID vmId);
}
