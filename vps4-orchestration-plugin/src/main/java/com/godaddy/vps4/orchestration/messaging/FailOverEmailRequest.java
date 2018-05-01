package com.godaddy.vps4.orchestration.messaging;

public class FailOverEmailRequest {
    public String shopperId;
    public String accountName;
    public boolean isFullyManaged;

    public FailOverEmailRequest() {
    }

    public FailOverEmailRequest(String shopperId, String accountName, boolean isFullyManaged) {
        this.shopperId = shopperId;
        this.accountName = accountName;
        this.isFullyManaged = isFullyManaged;
    }
}
