package com.godaddy.vps4.phase3.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class AddMonitoringTest implements VmTest {
    private static final Logger logger = LoggerFactory.getLogger(AddMonitoringTest.class);
    private final int ADD_MONITORING_TIMEOUT_SECONDS = 240;

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient apiClient = vm.getClient();
        Vps4RemoteAccessClient client = vm.remote();

        assert(!client.hasPanoptaAgent());
        assert(apiClient.getServerDetails(vm.vmId).get("monitoringAgent") == null);

        long actionId = apiClient.installMonitoring(vm.vmId);
        logger.debug("Wait for ADD_MONITORING on vm {}, via action id: {}", vm.vmId, actionId);
        apiClient.pollForVmActionComplete(vm.vmId, actionId, ADD_MONITORING_TIMEOUT_SECONDS);

        assert(client.hasPanoptaAgent());
        assert(apiClient.getServerDetails(vm.vmId).get("monitoringAgent") != null);
    }

    @Override
    public String toString() {
        return "Add Monitoring Test";
    }
}
