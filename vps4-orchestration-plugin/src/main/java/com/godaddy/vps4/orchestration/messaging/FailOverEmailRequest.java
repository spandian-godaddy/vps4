package com.godaddy.vps4.orchestration.messaging;

import java.util.UUID;

public class FailOverEmailRequest {
    public UUID customerId;
    public String accountName;
    public boolean isManaged;

    public FailOverEmailRequest() {
    }

    public FailOverEmailRequest(UUID customerId, String accountName, boolean isManaged) {
        this.customerId = customerId;
        this.accountName = accountName;
        this.isManaged = isManaged;
    }
}
