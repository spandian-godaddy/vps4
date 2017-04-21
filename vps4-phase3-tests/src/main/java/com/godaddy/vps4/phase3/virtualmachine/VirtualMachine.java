package com.godaddy.vps4.phase3.virtualmachine;

import java.util.UUID;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.ssh.Vps4SshClient;

public class VirtualMachine {

    public final UUID vmId;

    public final String imageName;

    final Vps4ApiClient apiClient;

    final VirtualMachinePool vmPool;

    final String defaultUsername;

    final String defaultPassword;

    public VirtualMachine(
            VirtualMachinePool vmPool,
            Vps4ApiClient apiClient,
            String imageName,
            String defaultUsername,
            String defaultPassword,
            UUID vmId){
        this.apiClient = apiClient;
        this.imageName = imageName;
        this.vmPool = vmPool;
        this.vmId = vmId;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    public Vps4ApiClient getClient() {
        return apiClient;
    }

    public Vps4SshClient ssh() {
        return new Vps4SshClient(apiClient, defaultUsername, defaultPassword);
    }

    public Vps4SshClient ssh(String username, String password) {
        return new Vps4SshClient(apiClient, username, password);
    }

    public void release() {
        vmPool.offer(this);
    }

    public void destroy() {
        vmPool.destroy(this);
    }
    
    public String toString(){
        return "VMID: " + vmId + ", Image Name: " + imageName;
    }

}
