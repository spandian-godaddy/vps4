package com.godaddy.vps4.phase3.tests;

import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class ConsoleUrlTest implements VmTest {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleUrlTest.class);
    private final int CREATE_CONSOLE_URL_TIMEOUT_SECONDS = 240;

    @Override
    public void execute(VirtualMachine vm) {
        Vps4ApiClient vps4Client = vm.getClient();
        if (vm.isDed()) {
            JSONObject createConsoleUrlJson = vps4Client.createConsoleUrl(vm.vmId);
            long actionId = (long)createConsoleUrlJson.get("id");
            logger.debug("Wait for CREATE_CONSOLE to finish on vm {}, via action id: {}", vm.vmId, actionId);
            vps4Client.pollForVmActionComplete(vm.vmId, actionId, CREATE_CONSOLE_URL_TIMEOUT_SECONDS);
        }
        String url = vps4Client.getConsoleUrl(vm.vmId);
        logger.debug("Console url for vm {} is: {}", vm.vmId, url);
        assert(url.startsWith("https://"));
    }

    @Override
    public String toString(){
        return "Console Url Test";
    }
}