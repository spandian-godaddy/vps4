package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.godaddy.vps4.vm.VmOutage;

public class VmOutageEmailRequest {
    public String accountName;
    public String ipAddress;
    public UUID orionGuid;
    public UUID customerId;
    public UUID vmId;
    public boolean managed;
    public VmOutage vmOutage;

    public VmOutageEmailRequest() {
        // empty constructor
    }

    public VmOutageEmailRequest(String accountName, String ipAddress, UUID orionGuid, UUID customerId, UUID vmId,
                                boolean managed, VmOutage vmOutage) {
        this.accountName = accountName;
        this.ipAddress = ipAddress;
        this.orionGuid = orionGuid;
        this.customerId = customerId;
        this.vmId = vmId;
        this.managed = managed;
        this.vmOutage = vmOutage;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
