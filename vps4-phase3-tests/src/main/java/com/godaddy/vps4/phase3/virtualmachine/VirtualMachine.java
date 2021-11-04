package com.godaddy.vps4.phase3.virtualmachine;

import java.util.UUID;

import org.json.simple.JSONObject;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.remote.Vps4SshClient;
import com.godaddy.vps4.phase3.remote.Vps4WinexeClient;

public class VirtualMachine {

    public final UUID vmId;
    public final UUID orionGuid;
    public final String imageName;
    final Vps4ApiClient apiClient;
    final Vps4ApiClient adminClient;
    final VirtualMachinePool vmPool;
    String defaultUsername;
    String defaultPassword;

    public VirtualMachine(
            VirtualMachinePool vmPool,
            Vps4ApiClient apiClient,
            Vps4ApiClient adminClient,
            String imageName,
            String defaultUsername,
            String defaultPassword,
            UUID vmId,
            UUID orionGuid){
        this.apiClient = apiClient;
        this.adminClient = adminClient;
        this.imageName = imageName;
        this.vmPool = vmPool;
        this.vmId = vmId;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
        this.orionGuid = orionGuid;
    }

    public Vps4ApiClient getClient() {
        return apiClient;
    }

    public Vps4ApiClient getAdminClient() { return adminClient; }

    public String getUsername() {
        return this.defaultUsername;
    }

    public void setUsername(String newUsername) { this.defaultUsername = newUsername; }

    public String getPassword() {
        return this.defaultPassword;
    }

    public void setPassword(String newPassword) {
        this.defaultPassword = newPassword;
    }

    public boolean isWindows() {
        JSONObject image = apiClient.getImage(imageName);
        return image.get("operatingSystem").toString().equalsIgnoreCase("WINDOWS");
    }

    public boolean isDed() {
        JSONObject image = apiClient.getImage(imageName);
        JSONObject serverType = (JSONObject) image.get("serverType");
        return serverType.get("serverType").toString().equalsIgnoreCase("DEDICATED");
    }

    public boolean isPlatformOptimizedHosting() {
        JSONObject image = apiClient.getImage(imageName);
        JSONObject serverType = (JSONObject) image.get("serverType");
        return serverType.get("platform").toString().equalsIgnoreCase("OPTIMIZED_HOSTING");
    }

    public Vps4RemoteAccessClient remote() {
        // use adminClient instead of apiClient because shopper cannot get VM primary IP for remote access when VM is suspended
        String primaryIpAddress = adminClient.getVmPrimaryIp(vmId);
        if (isWindows()) {
            return new Vps4WinexeClient(primaryIpAddress, defaultUsername, defaultPassword);
        }
        return new Vps4SshClient(primaryIpAddress, defaultUsername, defaultPassword);
    }

    public void release() {
        vmPool.offer(this);
    }

    public void destroy() {
        vmPool.destroy(this);
    }

    @Override
    public String toString(){
        return "VMID: " + vmId + ", Image Name: " + imageName;
    }

}
