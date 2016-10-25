package com.godaddy.vps4.web.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.hfs.Vm;
import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmAddress;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;

import gdg.hfs.vhfs.network.IpAddress;

public class FakeProvisionVmWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FakeProvisionVmWorker.class);

    final CreateVmAction action;

    public FakeProvisionVmWorker(CreateVmAction action) {
        this.action = action;
    }

    @Override
    public void run() {
        action.step = CreateVmStep.StartingServerSetup;
        logger.info("begin provision vm for request: {}", action.hfsProvisionRequest);
        action.status = ActionStatus.IN_PROGRESS;

        // IP
        action.step = CreateVmStep.RequestingIPAddress;
        sleep(1000);

        IpAddress ip = new IpAddress();
        ip.address = "4.3.2.1";

        action.ip = ip;

        // VM
        action.step = CreateVmStep.RequestingServer;
        sleep(3000);

        Vm vm = new Vm();
        vm.address = new VmAddress();
        vm.address.ip_address = "1.2.3.4";

        action.vm = vm;

        // Bind IP
        action.step = CreateVmStep.ConfiguringNetwork;
        sleep(2000);

        /*
        virtualMachineService.provisionVirtualMachine(action.vm.vmId, provisionRequest.orionGuid, provisionRequest.name, project.getProjectId(), spec.specId,
                request.managedLevel, imageId);
        */
        action.step = CreateVmStep.SetupComplete;
        action.status = ActionStatus.COMPLETE;

        logger.info("provision vm finished with status {} for action: {}", action);
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
