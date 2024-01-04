package com.godaddy.vps4.orchestration.vm.provision;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ProvisionVmInfo;

public class ProvisionRequest extends VmActionRequest {
    public ProvisionVmInfo vmInfo;
    public String shopperId;
    public String serverName;
    public byte[] encryptedPassword;
    public UUID orionGuid;
    public String sgid;
    public String image_name;
    public String rawFlavor;
    public String username;
    public String zone;
    public String privateLabelId;
    public List<Integer> intentIds;
    public String intentOtherDescription;
}