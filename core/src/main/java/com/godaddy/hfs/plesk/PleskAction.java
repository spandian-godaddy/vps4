package com.godaddy.hfs.plesk;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class PleskAction {
    public long actionId;
    public long serverId;
    public long vmId;
    public long cncRequestId;
    public String workflowId;
    public int actionType;
    public String createdAt;
    public String modifiedAt;
    public String completedAt;
    public Status status;
    public String message;
    public String responsePayload;
    public String project;

    @Override
    public String toString() { return ToStringBuilder.reflectionToString(this); }

    public enum ActionType {
        ImagePrep(1),
        ImageConfig(2),
        LicenseRefresh(3),
        LicenseActivate(4),
        LicenseRelease(5),
        LicenseUpdateIP(6),
        RequestAccess(7),
        RequestSiteList(8), 
        AdminPassUpdate(9),
        SetOutgoingEMailIP(10);

        public final int id;

        ActionType(int id) {
            this.id = id;
        }
    }

    public enum Status {
        NEW(1),
        IN_PROGRESS(2),
        COMPLETE(3),
        FAILED(4);

        public final int id;

        private Status(int id) {
            this.id = id;
        }
    }

    private static final Status[] statusById = new Status[Status.values().length + 1];

    static {
        for (Status status : Status.values()) {
            statusById[status.id] = status;
        }
    }
}
