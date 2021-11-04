package com.godaddy.vps4.phase3.tests;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.remote.Vps4RemoteAccessClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class ActivationTest implements VmTest {
    @Override
    public void execute(VirtualMachine vm) {
        Vps4RemoteAccessClient client = vm.remote();
        assert(client.isActivated());
    }

    @Override
    public String toString(){
        return "Windows Activation Test";
    }
}
