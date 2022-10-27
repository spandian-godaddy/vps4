package com.godaddy.vps4.orchestration.messaging;

import java.util.UUID;

public class SetupCompletedEmailRequest {
    public UUID customerId;
    public boolean isManaged;
    public UUID orionGuid;
    public String serverName;
    public String ipAddress;

    public SetupCompletedEmailRequest() {
    }

    public SetupCompletedEmailRequest(UUID customerId, boolean isManaged, UUID orionGuid, String serverName, String ipAddress) {
        this.customerId = customerId;
        this.isManaged = isManaged;
        this.orionGuid = orionGuid;
        this.serverName = serverName;
        this.ipAddress = ipAddress;
    }
}
