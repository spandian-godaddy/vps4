package com.godaddy.vps4.credit;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.security.Vps4User;

public interface CreditService {

    void createVirtualMachineCredit(UUID orionGuid, String osType, String controlPanel,
            int tier, int managedLevel, int monitoring, String shopperId);

    VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid);

    List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId);

    void createCreditIfNoneExists(Vps4User vps4User);

    void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId);

    void unclaimVirtualMachineCredit(UUID orionGuid);
}
