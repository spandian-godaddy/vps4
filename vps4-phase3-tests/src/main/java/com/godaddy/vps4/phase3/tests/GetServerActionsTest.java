package com.godaddy.vps4.phase3.tests;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class GetServerActionsTest implements VmTest {
    private static final Logger logger = LoggerFactory.getLogger(GetServerActionsTest.class);

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();
        JSONObject getServerActions = vps4Client.getServerActions(vm.vmId);
        JSONArray actions = (JSONArray)getServerActions.get("results");
        logger.debug("Current count of server actions for vm {}: {}", vm.vmId, actions.size());
        assert(actions.size()>0);
    }

    @Override
    public String toString(){
        return "Get Server Actions Test";
    }
}