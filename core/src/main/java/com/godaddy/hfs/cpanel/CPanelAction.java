package com.godaddy.hfs.cpanel;

public class CPanelAction {
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

    @Override
    public String toString() {
        return "CPanelAction [actionId=" + actionId +
                ", serverId=" + serverId +
                ", vmId=" + vmId +
                ", actionType=" + actionType +
                ", cncRequestId=" + cncRequestId +
                ", workflowId=" + workflowId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", modifiedAt=" + modifiedAt +
                ", completedAt=" + completedAt +
                ", message=" + message +
                ", responsePayload=" + responsePayload +
                "]";
    }

    public enum ActionType {
        ImagePrep(1),
        ImageConfig(2),
        LicenseRefresh(3),
        LicenseActivate(4),
        LicenseRelease(5),
        LicenseUpdateIP(6),
        RequestAccess(7),
        RequestSiteList(8),
        GetCPanelPublicIp(9),
        LicenseReleaseByIP(10);

        public final int id;

        private ActionType(int id) {
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

    public static Status getStatusByid(int statusId) {
        return statusById[statusId];
    }

}
