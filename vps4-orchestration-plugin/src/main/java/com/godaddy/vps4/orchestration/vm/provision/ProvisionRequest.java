package com.godaddy.vps4.orchestration.vm.provision;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ProvisionVmInfo;

import java.util.UUID;

public class ProvisionRequest implements ActionRequest {
    public ProvisionVmInfo vmInfo;
    public String shopperId;
    public String serverName;
    public byte[] encryptedPassword;
    public long actionId;
    public UUID orionGuid;
    public String sgid;
    public String image_name;
    public String rawFlavor;
    public String username;
    public String zone;

    @Override
    public long getActionId() {
        return actionId;
    }

    @Override
    public void setActionId(long actionId) {
        this.actionId = actionId;
    }
}