package com.godaddy.vps4.orchestration.messaging;

public class FailOverEmailRequest {
    public String shopperId;
    public String accountName;
    public boolean isManaged;

    public FailOverEmailRequest() {
    }

    public FailOverEmailRequest(String shopperId, String accountName, boolean isManaged) {
        this.shopperId = shopperId;
        this.accountName = accountName;
        this.isManaged = isManaged;
    }
}
