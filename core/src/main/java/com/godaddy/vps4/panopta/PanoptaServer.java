package com.godaddy.vps4.panopta;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PanoptaServer {
    public String partnerCustomerKey;
    public long serverId;
    public String serverKey;

    public PanoptaServer(String partnerCustomerKey, long serverId, String serverKey) {
        this.partnerCustomerKey = partnerCustomerKey;
        this.serverId = serverId;
        this.serverKey = serverKey;
    }

    public String getPartnerCustomerKey() {
        return partnerCustomerKey;
    }

    public long getServerId() {
        return serverId;
    }

    public String getServerKey() {
        return serverKey;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
