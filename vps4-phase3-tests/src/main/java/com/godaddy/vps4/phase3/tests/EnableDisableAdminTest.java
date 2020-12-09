package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class EnableDisableAdminTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(EnableDisableAdminTest.class);

    private int TOGGLE_ADMIN_TIMEOUT_SECONDS = 240;

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();

        long enableAdminActionId = vps4Client.enableAdmin(vm.vmId, vm.getUsername());
        logger.debug("Wait for ENABLE_ADMIN on vm {}, via action id: {}", vm, enableAdminActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, enableAdminActionId, TOGGLE_ADMIN_TIMEOUT_SECONDS);

        logger.debug("Verify admin enabled on vm {}", vm);
        Vps4RemoteAccessClient client = vm.remote();
        assert(client.hasAdminPrivilege(vm.vmId));

        long disableAdminActionId = vps4Client.disableAdmin(vm.vmId, vm.getUsername());
        logger.debug("Wait for DISABLE_ADMIN on vm {}, via action id: {}", vm, disableAdminActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, disableAdminActionId, TOGGLE_ADMIN_TIMEOUT_SECONDS);

        // Admin access is required for Winexe so we cannot use Winexe to verify that admin access is disabled
        // Skip verifying admin disabled on Windows for now
        if (!vm.isWindows()) {
            logger.debug("Verify admin disabled on vm {}", vm);
            assert(!client.hasAdminPrivilege(vm.vmId));
        }
    }

    @Override
    public String toString(){
        return "Enable Admin & Disable Admin Test";
    }
}