package com.godaddy.vps4.panopta;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;

public class PanoptaCustomerRequest {
    private UUID orionGuid;
    private String shopperId; // this is the name field in the request to panopta
    private String partnerCustomerKey;
    private String emailAddress;
    private String panoptaPackage;
    @Inject
    private Config config;
    @Inject
    private CreditService creditService;

    public PanoptaCustomerRequest(CreditService creditService, Config config) {
        this.creditService = creditService;
        this.config = config;
    }

    public PanoptaCustomerRequest createPanoptaCustomerRequest(UUID orionGuid) {

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        this.shopperId = credit.getShopperId();
        partnerCustomerKey = config.get("panopta.api.partner.customer.key.prefix") + orionGuid;
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

    public UUID getOrionGuid() {
        return orionGuid;
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
