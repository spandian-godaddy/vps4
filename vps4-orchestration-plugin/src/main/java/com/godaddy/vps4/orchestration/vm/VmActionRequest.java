package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.ActionRequest;

public class VmActionRequest implements ActionRequest {
    public long actionId;
    public long hfsVmId;

    @Override
    public long getActionId() {
        return actionId;
    }

    @Override
    public void setActionId(long actionId) {
        this.actionId = actionId;
    }

}
