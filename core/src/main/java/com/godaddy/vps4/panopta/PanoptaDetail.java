package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PanoptaDetail {
    private UUID vmId;
    private String partnerCustomerKey;
    private String customerKey;
    private long serverId;
    private String serverKey;
    private Instant serverCreated;
    private Instant serverDestroyed;
    private String templateId;

    public PanoptaDetail() {}

    public PanoptaDetail(UUID vmId, String partnerCustomerKey, String customerKey,
            long serverId, String serverKey, Instant created, Instant destroyed, String templateId) {
        this.vmId = vmId;
        this.partnerCustomerKey = partnerCustomerKey;
        this.customerKey = customerKey;
        this.serverId = serverId;
        this.serverKey = serverKey;
        this.serverCreated = created;
        this.serverDestroyed = destroyed;
        this.templateId = templateId;
    }

    public UUID getVmId() {
        return vmId;
    }

    public String getPartnerCustomerKey() {
        return partnerCustomerKey;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public long getServerId() {
        return serverId;
    }

    public String getServerKey() {
        return serverKey;
    }

    public Instant getServerCreated() {
        return serverCreated;
    }

    public Instant getServerDestroyed() {
        return serverDestroyed;
    }

    public String getTemplateId() { return templateId; }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
