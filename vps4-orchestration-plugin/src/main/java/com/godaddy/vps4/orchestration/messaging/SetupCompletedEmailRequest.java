package com.godaddy.vps4.orchestration.messaging;

import java.util.UUID;

public class SetupCompletedEmailRequest {
    public String shopperId;
    public boolean isManaged;
    public UUID orionGuid;
    public String serverName;
    public String ipAddress;

    public SetupCompletedEmailRequest() {
    }

    public SetupCompletedEmailRequest(String shopperId, boolean isManaged, UUID orionGuid, String serverName, String ipAddress) {
        this.shopperId = shopperId;
        this.isManaged = isManaged;
        this.orionGuid = orionGuid;
        this.serverName = serverName;
        this.ipAddress = ipAddress;
    }
}
