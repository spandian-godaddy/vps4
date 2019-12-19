package com.godaddy.vps4.panopta;

import java.util.UUID;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

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

    @Inject
    public PanoptaCustomerRequest(VirtualMachineService virtualMachineService, CreditService creditService, Config config) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.config = config;
    }

    public PanoptaCustomerRequest createPanoptaCustomerRequest(String shopperId) {

        this.shopperId = shopperId;
        this.partnerCustomerKey = config.get("panopta.api.partner.customer.key.prefix") + shopperId;
        this.emailAddress = config.get("panopta.api.customer.email", "dev-vps4@godaddy.com");
        this.panoptaPackage = config.get("panopta.api.package");
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
