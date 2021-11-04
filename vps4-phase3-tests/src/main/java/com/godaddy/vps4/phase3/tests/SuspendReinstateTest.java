package com.godaddy.vps4.phase3.tests;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class SuspendReinstateTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(SuspendReinstateTest.class);

    private int SUSPEND_TIMEOUT_SECONDS = 480;
    private int REINSTATE_TIMEOUT_SECONDS = 480;

    @Override
    public void execute(VirtualMachine vm) {
        // only certain roles such as admin can suspend/reinstate, thus getAdminClient
        Vps4ApiClient vps4AdminClient = vm.getAdminClient();
        Vps4RemoteAccessClient remoteAccessClient = vm.remote();

        // Admin access is required for Winexe
        if (vm.isWindows()) {
            logger.debug("Turning on admin access for user {} on vm {}", vm.getUsername(), vm.vmId);
            vps4AdminClient.enableAdmin(vm.vmId, vm.getUsername());
        }

        long abuseSuspendActionId = vps4AdminClient.abuseSuspend(vm.vmId);
        logger.debug("Wait for abuse suspend on vm {}, via action id: {}", vm.vmId, abuseSuspendActionId);
        vps4AdminClient.pollForVmActionComplete(vm.vmId, abuseSuspendActionId, SUSPEND_TIMEOUT_SECONDS);

        JSONObject creditJson = vps4AdminClient.getCredit(vm.orionGuid);
        logger.debug("Verify credit after suspension on vm {}: {}", vm.vmId, creditJson.toString());
        assert(creditJson.get("accountStatus").equals("ABUSE_SUSPENDED"));
        assert(creditJson.get("abuseSuspendedFlagSet").equals(true));

        logger.debug("Verify remote connection failure after suspension on vm {}", vm.vmId);
        assert(!remoteAccessClient.checkConnection());

        long reinstateActionId = vps4AdminClient.reinstateAbuseSuspend(vm.vmId);
        logger.debug("Wait for reinstate abuseSuspend on vm {}, via action id: {}", vm.vmId, reinstateActionId);
        vps4AdminClient.pollForVmActionComplete(vm.vmId, reinstateActionId, REINSTATE_TIMEOUT_SECONDS);
        // Poll until agent reports OK to ensure VM is online after reinstatement
        vps4AdminClient.pollForVmAgentStatusOK(vm.vmId, REINSTATE_TIMEOUT_SECONDS);

        JSONObject creditJson2 = vps4AdminClient.getCredit(vm.orionGuid);
        logger.debug("Verify credit after reinstatement on vm {}: {}", vm.vmId, creditJson2.toString());
        assert(creditJson2.get("accountStatus").equals("ACTIVE"));
        assert(creditJson2.get("abuseSuspendedFlagSet").equals(false));

        logger.debug("Verify remote connection success after reinstatement on vm {}", vm.vmId);
        assert(remoteAccessClient.checkConnection());
    }

    @Override
    public String toString(){
        return "Suspend & Reinstate Test";
    }
}
