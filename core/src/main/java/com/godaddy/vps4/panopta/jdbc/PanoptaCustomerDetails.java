package com.godaddy.vps4.panopta.jdbc;

import java.time.Instant;

public class PanoptaCustomerDetails {
    private String partnerCustomerKey;
    private String customerKey;
    private Instant created;
    private Instant destroyed;

    public String getPartnerCustomerKey() {
        return partnerCustomerKey;
    }

    public void setPartnerCustomerKey(String partnerCustomerKey) {
        this.partnerCustomerKey = partnerCustomerKey;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getDestroyed() {
        return destroyed;
    }

    public void setDestroyed(Instant destroyed) {
        this.destroyed = destroyed;
    }
}
