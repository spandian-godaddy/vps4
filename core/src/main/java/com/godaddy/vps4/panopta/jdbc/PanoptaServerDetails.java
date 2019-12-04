package com.godaddy.vps4.panopta.jdbc;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PanoptaServerDetails {
    private String partnerCustomerKey;
    private UUID vmId;
    private long serverId;
    private String serverKey;
    private Instant created;
    private Instant destroyed;

    public String getPartnerCustomerKey() {
        return partnerCustomerKey;
    }

    public void setPartnerCustomerKey(String partnerCustomerKey) {
        this.partnerCustomerKey = partnerCustomerKey;
    }

    public UUID getVmId() {
        return vmId;
    }

    public void setVmId(UUID vmId) {
        this.vmId = vmId;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
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
