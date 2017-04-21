package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class StopStartVmTest implements VmTest {
    
    private static final Logger logger = LoggerFactory.getLogger(StopStartVmTest.class);

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();

        String stopVmActionId = vps4Client.stopVm(vm.vmId);
        logger.debug("Wait for shutdown on vm {}", vm);
        vps4Client.pollForVmActionComplete(vm.vmId, stopVmActionId, 240);

        Vps4SshClient sshClient = vm.ssh();
        assert(!sshClient.checkConnection(vm.vmId));

        String startVmActionId = vps4Client.startVm(vm.vmId);
        logger.debug("Wait for startup on vm {}", vm);
        vps4Client.pollForVmActionComplete(vm.vmId, startVmActionId, 240);
        
        assert(sshClient.checkConnection(vm.vmId));
    }
    
    @Override
    public String toString(){
        return "Stop/Start VM Test";
    }
}
