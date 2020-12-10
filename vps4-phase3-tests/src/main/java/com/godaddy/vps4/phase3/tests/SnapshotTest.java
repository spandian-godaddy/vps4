package com.godaddy.vps4.phase3.tests;

import java.util.UUID;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
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

        // Do not run snapshot test on dedicated servers
        if (vm.isDed()) {
            logger.debug("Skipping SnapshotTest on vm {}", vm);
            return;
        }

        Vps4ApiClient vps4Client = vm.getClient();

        // Sometimes even when windows VM becomes accessible through winexe, HFS agent status might not be reporting "OK" yet.
        // Poll until agent reports OK to avoid "Agent is down. Refusing to take snapshot" error when creating snapshot later
        vps4Client.pollForVmAgentStatusOK(vm.vmId, TROUBLESHOOT_TIMEOUT_SECONDS);

        JSONObject snapshotActionId = vps4Client.snapshotVm(vm.vmId);
        long actionId = (long)snapshotActionId.get("id");
        UUID snapshotId = UUID.fromString((String)snapshotActionId.get("snapshotId"));
        logger.debug("Wait for snapshot id {} on vm {}, via action id: {}", snapshotId, vm, actionId);
        vps4Client.pollForSnapshotActionComplete(vm.vmId, snapshotId, actionId, SNAPSHOT_TIMEOUT_SECONDS);

        String snapshotStatus = vps4Client.getSnapshotStatus(vm.vmId, snapshotId);
        assert(snapshotStatus.equals("LIVE"));
    }
    
    @Override
    public String toString() {
        return "Snapshot VM Test";
    }
}
