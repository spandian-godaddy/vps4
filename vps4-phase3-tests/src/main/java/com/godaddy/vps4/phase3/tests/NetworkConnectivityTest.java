package com.godaddy.vps4.phase3.tests;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class NetworkConnectivityTest implements VmTest {
    @Override
    public void execute(VirtualMachine vm) {
        Vps4RemoteAccessClient client = vm.remote();
        assert(client.canPing("godaddy.com"));
        assert(client.canPing("google.com"));

        assert !vm.isWindows() || client.isRdpRunning();
    }

    @Override
    public String toString(){
        return "Network Connectivity Test";
    }
}
