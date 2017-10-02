package com.godaddy.vps4.credit;

import java.util.List;
import java.util.UUID;

public interface CreditService {

    void createVirtualMachineCredit(UUID orionGuid, String osType, String controlPanel,
            int tier, int managedLevel, int monitoring, String shopperId);

    VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid);

    List<VirtualMachineCredit> getUnclaimedVirtualMachineCredits(String shopperId);

    List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId);

    void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId, UUID productId);

    void unclaimVirtualMachineCredit(UUID orionGuid);

    void setCommonName(UUID orionGuid, String newName);
}
