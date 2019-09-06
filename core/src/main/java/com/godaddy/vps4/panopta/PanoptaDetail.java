package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.UUID;

public class PanoptaDetail {
    private long panoptaDetailId;
    private UUID vmId;
    private String partnerCustomerKey;
    private String customerKey;
    private int serverId;
    private String serverKey;
    private Instant created;
    private Instant destroyed;

    public PanoptaDetail(long panoptaDetailId, UUID vmId, String partnerCustomerKey, String customerKey,
            int serverId, String serverKey, Instant created, Instant destroyed) {
        this.panoptaDetailId = panoptaDetailId;
        this.vmId = vmId;
        this.partnerCustomerKey = partnerCustomerKey;
        this.customerKey = customerKey;
        this.serverId = serverId;
        this.serverKey = serverKey;
        this.created = created;
        this.destroyed = destroyed;
    }

    public long getPanoptaDetailId() {
        return panoptaDetailId;
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

    public int getServerId() {
        return serverId;
    }

    public String getServerKey() {
        return serverKey;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getDestroyed() {
        return destroyed;
    }
}
