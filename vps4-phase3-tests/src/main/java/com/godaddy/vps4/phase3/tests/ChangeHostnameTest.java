package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class ChangeHostnameTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(ChangeHostnameTest.class);

    final String newHostname;

    private int HOSTNAME_TIMEOUT_SECONDS = 240;
    private int RESTART_TIMEOUT_SECONDS = 480;
    private int WAIT_AFTER_RESTART_MILLISECONDS = 30000;

    public ChangeHostnameTest(String newHostname) {
        this.newHostname = newHostname;
    }

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();

        // Admin access is required for Winexe
        if (vm.isWindows()) {
            logger.debug("Turning on admin access for user {} on vm {}", vm.getUsername(), vm);
            vps4Client.enableAdmin(vm.vmId, vm.getUsername());
        }

        long setHostnameActionId = vps4Client.setHostname(vm.vmId, newHostname);
        logger.debug("Wait for change hostname on vm {}, via action id: {}", vm, setHostnameActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, setHostnameActionId, HOSTNAME_TIMEOUT_SECONDS);

        // a reboot is only needed for windows to apply hostname change; reboot not required for linux
        if (vm.isWindows()) {
            long restartVmActionId = vps4Client.restartVm(vm.vmId);
            logger.debug("Wait for restart on vm {}, via action id: {}", vm, restartVmActionId);
            vps4Client.pollForVmActionComplete(vm.vmId, restartVmActionId, RESTART_TIMEOUT_SECONDS);

            try {
                // A brief pause before trying remote connections
                Thread.sleep(WAIT_AFTER_RESTART_MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("Error during start stop test sleeping, pre-remote check", e);
            }
        }

        Vps4RemoteAccessClient client = vm.remote();
        logger.debug("check hostname on vm {}", vm);
        assert client.checkHostname(vm.vmId, newHostname);
    }

    @Override
    public String toString(){
        return "Change Hostname Test";
    }
}