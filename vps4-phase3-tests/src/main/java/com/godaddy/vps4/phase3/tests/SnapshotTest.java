package com.godaddy.vps4.phase3.tests;

import static com.godaddy.vps4.phase3.RunSomeTests.randomPassword;

import java.util.UUID;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapshotTest implements VmTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SnapshotTest.class);

    private final int SNAPSHOT_TIMEOUT_SECONDS = 2100;
    private final int TROUBLESHOOT_TIMEOUT_SECONDS = 240;

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();
        // Do not run snapshot test on dedicated servers
        if (vm.isDed()) {
            logger.debug("Skipping SnapshotTest on dedicated server {}", vm.vmId);
            return;
        }

        // Sometimes even when windows VM becomes accessible through winexe, HFS agent status might not be reporting "OK" yet.
        // Poll until agent reports OK to avoid "Agent is down. Refusing to take snapshot" error when creating snapshot later
        vps4Client.pollForVmAgentStatusOK(vm.vmId, TROUBLESHOOT_TIMEOUT_SECONDS);

        // Create on-demand backup
        JSONObject snapshotAction = vps4Client.snapshotVm(vm.vmId);
        long snapshotActionId = (long)snapshotAction.get("id");
        UUID snapshotId = UUID.fromString((String)snapshotAction.get("snapshotId"));
        logger.debug("Wait for snapshot id {} on vm {}, via action id: {}", snapshotId, vm.vmId, snapshotActionId);
        vps4Client.pollForSnapshotActionComplete(vm.vmId, snapshotId, snapshotActionId, SNAPSHOT_TIMEOUT_SECONDS);

        // Verify backup is now in LIVE status
        String snapshotStatus = vps4Client.getSnapshotStatus(vm.vmId, snapshotId);
        logger.debug("Snapshot {} status: {}", snapshotId, snapshotStatus);
        assert(snapshotStatus.equals("LIVE"));

        // Restore from the freshly created backup. Note that password is only required for OpenStack not OptimizedHosting
        String newPassword = "";
        boolean isPlatformOH = vm.isPlatformOptimizedHosting();
        if(!isPlatformOH) {
            newPassword = randomPassword(14);
        }
        JSONObject restoreAction = vps4Client.restoreVm(vm.vmId, snapshotId, newPassword);
        long restoreActionId = (long)restoreAction.get("id");
        logger.debug("Wait for snapshot id {} to be restored on vm {}, via action id: {}", snapshotId, vm.vmId, restoreActionId);
        vps4Client.pollForVmActionComplete(vm.vmId, restoreActionId, SNAPSHOT_TIMEOUT_SECONDS);

        // Verify remote connection. Note that password is updated for OpenStack VM
        if(!isPlatformOH) {
            vm.setPassword(newPassword);
        }
        // Admin access is required for Winexe
        if (vm.isWindows()) {
            logger.debug("Turning on admin access for user {} on vm {}", vm.getUsername(), vm.vmId);
            long enableAdminActionId = vps4Client.enableAdmin(vm.vmId, vm.getUsername());
            logger.debug("Wait for ENABLE_ADMIN on vm {}, via action id: {}", vm.vmId, enableAdminActionId);
            vps4Client.pollForVmActionComplete(vm.vmId, enableAdminActionId, TROUBLESHOOT_TIMEOUT_SECONDS);
        }
        logger.debug("Verify remote connection success after restoring snapshot {} on vm {}", snapshotId, vm.vmId);
        Vps4RemoteAccessClient client = vm.remote();
        assert(client.checkConnection());
    }
    
    @Override
    public String toString() {
        return "Snapshot & Restore VM Test";
    }
}
