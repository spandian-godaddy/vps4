package com.godaddy.vps4.credit;

import java.util.List;
import java.util.UUID;

public interface Vps4LocalCreditService {
    void insertCredit(VirtualMachineCredit credit);
    
    VirtualMachineCredit getCredit(UUID creditId);

    VirtualMachineCredit getCreditByVmId(UUID vmId);

    List<VirtualMachineCredit> getCreditByShopperId(String shopperId);

    List<VirtualMachineCredit> getCreditByCustomerId(UUID customerId);

    void updateCredit(VirtualMachineCredit credit);
    
    void deleteCredit(UUID creditId);
}
