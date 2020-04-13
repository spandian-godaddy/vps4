package com.godaddy.vps4.appmonitors;

import java.time.Instant;
import java.util.UUID;

public class VmActionData {
    public long actionId;
    public UUID vmId;
    public long hfsVmId;
    public String actionType;
    public Instant created;
    public UUID commandId;
    public String initiatedBy;
}
