package com.godaddy.vps4.phase3.tests;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class AddSupportUserTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(AddSupportUserTest.class);
    private final int ADD_SUPPORT_USER_TIMEOUT_SECONDS = 240;

    @Override
    public void execute(VirtualMachine vm) {
        // only admins can add support users, thus getAdminClient
        Vps4ApiClient vps4AdminClient = vm.getAdminClient();
        JSONObject addSupportUserJson = vps4AdminClient.addSupportUser(vm.vmId);
        String message = addSupportUserJson.get("message").toString();
        long actionId = (long)addSupportUserJson.get("id");
        String originalUsername= vm.getUsername();
        String originalPwd = vm.getPassword();
        try {
            JSONObject messageJson = (JSONObject) new JSONParser().parse(message);
            String username = messageJson.get("Username").toString();
            String password = messageJson.get("Password").toString();

            logger.debug("Wait for ADD_SUPPORT_USER ({}:{}) to finish on vm {}, via action id: {}", username, password, vm, actionId);
            vps4AdminClient.pollForVmActionComplete(vm.vmId, actionId, ADD_SUPPORT_USER_TIMEOUT_SECONDS);

            logger.debug("Verify remote connection on vm {} using new support user creds", vm);
            vm.setUsername(username);
            vm.setPassword(password);
            Vps4RemoteAccessClient client1 = vm.remote();
            assert(client1.checkConnection(vm.vmId));
            logger.debug("Verify remote connection on vm {} using original user creds", vm);
            vm.setUsername(originalUsername);
            vm.setPassword(originalPwd);
            Vps4RemoteAccessClient client2 = vm.remote();
            assert(client2.checkConnection(vm.vmId));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        return "Add Support User Test";
    }
}