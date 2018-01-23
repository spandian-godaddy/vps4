package com.godaddy.vps4.appmonitors;

import java.util.UUID;

public class VmActionData {
    public String actionId;
    public UUID commandId;
    public UUID vmId;
    public String action_type;

    public VmActionData(String actionId, UUID commandId, UUID vmId, String action_type) {
        this.actionId = actionId;
        this.commandId = commandId;
        this.vmId = vmId;
        this.action_type = action_type;
    }

    public VmActionData(String actionId, UUID commandId, UUID vmId) {
        this(actionId, commandId, vmId, "");
    }

}
