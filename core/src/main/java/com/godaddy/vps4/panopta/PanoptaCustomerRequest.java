package com.godaddy.vps4.panopta;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

public class PanoptaCustomerRequest {
    private UUID vmId;
    private String shopperId; // this is the name field in the request to panopta
    private String partnerCustomerKey;
    private String emailAddress;
    private String panoptaPackage;
    @Inject
    private Config config;
    @Inject
    VirtualMachineService virtualMachineService;
    @Inject
    CreditService creditService;

    public PanoptaCustomerRequest(VirtualMachineService virtualMachineService, CreditService creditService, Config config) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.config = config;
    }

    public PanoptaCustomerRequest createPanoptaCustomerRequest(UUID vmId) {

        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        this.shopperId = credit.getShopperId();
        partnerCustomerKey = config.get("panopta.api.partner.customer.key.prefix") + vmId;
        this.partnerCustomerKey = partnerCustomerKey;
        this.emailAddress = config.get("panopta.api.customer.email", "dev-vps4@godaddy.com");
        // commented out at the moment since we need to create the correct managed levels in Panopta.
        // customerRequest.panoptaPackage = config.get("panopta.api.package." + credit
        // .effectiveManagedLevel().toString());
        // defaulting to fully managed for now
        this.panoptaPackage =
                config.get("panopta.api.package.FULLY_MANAGED");
        return this;
    }

    public UUID getVmId() {
        return vmId;
    }

    public String getShopperId() {
        return shopperId;
    }

    public String getPartnerCustomerKey() {
        return partnerCustomerKey;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPanoptaPackage() {
        return panoptaPackage;
    }
}
