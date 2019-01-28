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

        long stopVmActionId = vps4Client.stopVm(vm.vmId);
        logger.debug("Wait for shutdown on vm {}, via action id: {}", vm, stopVmActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, stopVmActionId, 240);

        Vps4SshClient sshClient = vm.ssh();
        assert(!sshClient.checkConnection(vm.vmId));

        long startVmActionId = vps4Client.startVm(vm.vmId);
        logger.debug("Wait for startup on vm {}, via action id: {}", vm, startVmActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, startVmActionId, 240);

        try {
            // A brief pause before trying ssh connection
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            logger.error("Error during start stop test sleeping, pre-ssh check", e);
        }

        logger.debug("Verify ssh connection on vm {}", vm);
        assert(sshClient.checkConnection(vm.vmId));
    }

    @Override
    public String toString(){
        return "Stop/Start VM Test";
    }
}
