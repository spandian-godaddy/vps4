package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.ssh.Vps4SshClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class ChangeHostnameTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHostnameTest.class);

    final String newHostname;

    private int HOSTNAME_TIMEOUT_SECONDS = 240;
    private int RESTART_TIMEOUT_SECONDS = 240;

    public ChangeHostnameTest(String newHostname) {
        this.newHostname = newHostname;
    }

    @Override
    public void execute(VirtualMachine vm){
        Vps4ApiClient vps4Client = vm.getClient();

        long setHostnameActionId = vps4Client.setHostname(vm.vmId, newHostname);
        logger.debug("Wait for change hostname on vm {}", vm);
        vps4Client.pollForVmActionComplete(vm.vmId, setHostnameActionId, HOSTNAME_TIMEOUT_SECONDS);

        long restartVmActionId = vps4Client.restartVm(vm.vmId);
        logger.debug("Wait for restart on vm {}", vm);
        vps4Client.pollForVmActionComplete(vm.vmId, restartVmActionId, RESTART_TIMEOUT_SECONDS);

        try {
            // A brief pause before trying ssh connections
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error("Error during start stop test sleeping, pre-ssh check", e);
        }

        Vps4SshClient sshClient = vm.ssh();
        sshClient.assertCommandResult(vm.vmId, newHostname, "hostname;");
    }

    @Override
    public String toString(){
        return "Change Hostname Test";
    }
}