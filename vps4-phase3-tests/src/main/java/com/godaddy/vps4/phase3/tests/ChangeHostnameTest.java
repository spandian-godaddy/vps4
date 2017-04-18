package com.godaddy.vps4.phase3.tests;

import com.godaddy.vps4.phase3.VmTest;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.ssh.Vps4SshClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;

public class ChangeHostnameTest implements VmTest
{
    final String newHostname;

    public ChangeHostnameTest(String newHostname) {
        this.newHostname = newHostname;
    }

    @Override
    public void execute(VirtualMachine vm){
        Vps4ApiClient vps4Client = vm.getClient();

        String setHostnameActionId = vps4Client.setHostname(vm.vmId, newHostname);
        vps4Client.pollForVmActionComplete(vm.vmId, setHostnameActionId);

        String restartVmActionId = vps4Client.restartVm(vm.vmId);
        vps4Client.pollForVmActionComplete(vm.vmId, restartVmActionId, 240);

        Vps4SshClient sshClient = vm.ssh();
        sshClient.assertCommandResult(vm.vmId, newHostname, "hostname;");
    }
    
    @Override
    public String toString(){
        return "Change Hostname Test";
    }

}
