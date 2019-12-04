package com.godaddy.vps4.panopta.jdbc;

import java.time.Instant;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
