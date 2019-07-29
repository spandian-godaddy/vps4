package com.godaddy.vps4.panopta;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PanoptaCustomer {
    public String customerKey;
    public String partnerCustomerKey;

    public PanoptaCustomer(String customerKey, String partnerCustomerKey) {
        this.customerKey = customerKey;
        this.partnerCustomerKey = partnerCustomerKey;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getPartnerCustomerKey() {
        return partnerCustomerKey;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
