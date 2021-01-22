package com.godaddy.vps4.panopta;

import java.time.Instant;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PanoptaServer {
    public String partnerCustomerKey;
    public long serverId;
    public String serverKey;
    public String name;
    public String fqdn;
    public String serverGroup;
    public Status status;
    public Instant agentLastSynced;

    public enum Status {
        ACTIVE, SUSPENDED, DELETED;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public PanoptaServer(String partnerCustomerKey, long serverId, String serverKey, String name, String fqdn,
                         String serverGroup, Status status, Instant agentLastSynced) {
        this.partnerCustomerKey = partnerCustomerKey;
        this.serverId = serverId;
        this.serverKey = serverKey;
        this.name = name;
        this.fqdn = fqdn;
        this.serverGroup = serverGroup;
        this.status = status;
        this.agentLastSynced = agentLastSynced;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
