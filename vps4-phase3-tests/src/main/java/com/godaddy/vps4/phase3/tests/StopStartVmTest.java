package com.godaddy.vps4.phase3.tests;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.ssh.Vps4SshClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

/**
 * Note: ideally, implementations of VmTest should be thread-safe, in that they
 *       act on the 'vm' parameter, but do not update internal state
 *       in a non-thread-safe way
 *
 */
public class StopStartVmTest implements VmTest
{

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();

        String stopVmActionId = vps4Client.stopVm(vm.vmId);
        vps4Client.pollForVmActionComplete(vm.vmId, stopVmActionId, 240);

        Vps4SshClient sshClient = vm.ssh();
        assert(!sshClient.checkConnection(vm.vmId));

        String startVmActionId = vps4Client.startVm(vm.vmId);
        vps4Client.pollForVmActionComplete(vm.vmId, startVmActionId, 240);
        assert(sshClient.checkConnection(vm.vmId));
    }
    
    @Override
    public String toString(){
        return "Stop/Start VM Test";
    }
}
