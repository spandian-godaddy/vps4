package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class SetPasswordTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(SetPasswordTest.class);

    final String newPassword;

    private int SETPASSWORD_TIMEOUT_SECONDS = 240;

    public SetPasswordTest(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();

        // Admin access is required for Winexe
        if (vm.isWindows()) {
            logger.debug("Turning on admin access for user {} on vm {}", vm.getUsername(), vm);
            vps4Client.enableAdmin(vm.vmId, vm.getUsername());
        }

        long setPasswordActionId = vps4Client.setPassword(vm.vmId, vm.getUsername(), newPassword);
        logger.debug("Wait for set password to {} on vm {}, via action id: {}", newPassword, vm, setPasswordActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, setPasswordActionId, SETPASSWORD_TIMEOUT_SECONDS);

        logger.debug("Verify remote connection fails on vm {} using old password", vm);
        Vps4RemoteAccessClient client1 = vm.remote();
        assert(!client1.checkConnection(vm.vmId));
        logger.debug("Verify remote connection on vm {} using new password", vm);
        vm.setPassword(newPassword);
        Vps4RemoteAccessClient client2 = vm.remote();
        assert(client2.checkConnection(vm.vmId));
    }

    @Override
    public String toString(){
        return "Set password Test";
    }
}