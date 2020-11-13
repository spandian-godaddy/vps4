package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

/**
 * Note: ideally, implementations of VmTest should be thread-safe, in that they
 *       act on the 'vm' parameter, but do not update internal state
 *       in a non-thread-safe way
 *
 */
public class StopStartVmTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(StopStartVmTest.class);

    private int STOP_TIMEOUT_SECONDS = 240;
    private int START_TIMEOUT_SECONDS = 240;
    private int DED_RESTART_TIMEOUT_SECONDS = 480;
    private int WAIT_AFTER_START_MILLISECONDS = 30000;
    private int DED_WAIT_AFTER_RESTART_MILLISECONDS = 180000;

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();

        // Admin access is required for Winexe
        if (vm.isWindows()) {
            logger.debug("Turning on admin access for user {} on vm {}", vm.getUsername(), vm);
            vps4Client.enableAdmin(vm.vmId, vm.getUsername());
        }

        Vps4RemoteAccessClient client = vm.remote();
        if (vm.isDed()) {
            long restartActionId = vps4Client.restartVm(vm.vmId);
            vps4Client.pollForVmActionComplete(vm.vmId, restartActionId, DED_RESTART_TIMEOUT_SECONDS);
        }
        else {
            long stopVmActionId = vps4Client.stopVm(vm.vmId);
            logger.debug("Wait for shutdown on vm {}, via action id: {}", vm, stopVmActionId);
            vps4Client.pollForVmActionComplete(vm.vmId, stopVmActionId, STOP_TIMEOUT_SECONDS);

            assert(!client.checkConnection(vm.vmId));

            long startVmActionId = vps4Client.startVm(vm.vmId);
            logger.debug("Wait for startup on vm {}, via action id: {}", vm, startVmActionId);
            vps4Client.pollForVmActionComplete(vm.vmId, startVmActionId, START_TIMEOUT_SECONDS);
        }

        try {
            // Pause before trying remote connection to allow the server to finish starting up
            int waitTime = vm.isDed() ? DED_WAIT_AFTER_RESTART_MILLISECONDS: WAIT_AFTER_START_MILLISECONDS;
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            logger.error("Error during start stop test sleeping, pre-remote check", e);
        }

        logger.debug("Verify remote connection on vm {}", vm);
        assert(client.checkConnection(vm.vmId));
    }

    @Override
    public String toString(){
        return "Stop/Start VM Test";
    }
}
