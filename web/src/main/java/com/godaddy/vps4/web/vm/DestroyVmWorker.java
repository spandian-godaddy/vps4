package com.godaddy.vps4.web.vm;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Action;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.network.NetworkAction;
import com.godaddy.vps4.web.network.ReleaseIpWorker;
import com.godaddy.vps4.web.network.UnbindIpWorker;
import com.godaddy.vps4.web.vm.VmResource.DestroyVmAction;

import gdg.hfs.vhfs.network.NetworkService;

public class DestroyVmWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DestroyVmWorker.class);

    final VmService vmService;
    final DestroyVmAction action;
    final NetworkService hfsNetworkService;
    final com.godaddy.vps4.network.NetworkService vps4NetworkService;
    final ExecutorService threadPool;
    final VirtualMachineService virtualMachineService;

    public DestroyVmWorker(DestroyVmAction action, VmService vmService, NetworkService hfsNetworkService,
            com.godaddy.vps4.network.NetworkService vps4NetworkService, VirtualMachineService virtualMachineService,
            ExecutorService threadPool) {
        this.vmService = vmService;
        this.action = action;
        this.hfsNetworkService = hfsNetworkService;
        this.vps4NetworkService = vps4NetworkService;
        this.threadPool = threadPool;
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public void run() {

        logger.info("destroying VM {}", action.virtualMachine.vmId);

        unbindAndReleaseIps();

        if (action.status != Action.ActionStatus.ERROR) {
            VmAction hfsAction = vmService.destroyVm(action.virtualMachine.vmId);

            // wait for the HFS action to complete
            while (hfsAction.state.equals("REQUESTED") || hfsAction.state.equals("IN_PROGRESS")) {
                logger.info("waiting on VM to be destroyed: {}", hfsAction);

                // give the VM time to be destroyed
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    logger.warn("Interrupted while sleeping");
                    action.status = ActionStatus.ERROR;
                }

                hfsAction = vmService.getVmAction(action.virtualMachine.vmId, hfsAction.vmActionId);
            }

            if (hfsAction.state.equals("COMPLETE"))
                logger.info("VM destroyed: {}", hfsAction);
            else {
                logger.warn("VM destroy failed: {}", hfsAction);
                action.status = ActionStatus.ERROR;
            }
        }

        if (action.status != ActionStatus.ERROR) {
            virtualMachineService.destroyVirtualMachine(action.virtualMachine.vmId);
            action.status = ActionStatus.COMPLETE;
        }
    }

    private void unbindAndReleaseIps() {

        List<IpAddress> addresses = vps4NetworkService.listIpAddresses(action.virtualMachine.projectId);
        CompletionService<NetworkAction> unbindCompletionService = new ExecutorCompletionService<NetworkAction>(
                Executors.newFixedThreadPool(addresses.size()));
        int remainingFutures = 0;

        for (IpAddress address : addresses) {
            remainingFutures++;

            unbindCompletionService.submit(new UnbindIpWorker(new NetworkAction(address.ipAddressId), hfsNetworkService));
        }

        Future<NetworkAction> completedFuture;
        NetworkAction networkAction = null;

        while (remainingFutures > 0) {
            try {
                completedFuture = unbindCompletionService.take();
                remainingFutures--;

                networkAction = completedFuture.get();
                if (networkAction.status == ActionStatus.COMPLETE) {
                    threadPool
                            .submit(new ReleaseIpWorker(new NetworkAction(networkAction.addressId), hfsNetworkService, vps4NetworkService));
                }
                else {
                    action.status = Action.ActionStatus.ERROR;
                }
            }
            catch (ExecutionException | InterruptedException e) {
                logger.warn("Exception during unbind ip");
                action.status = ActionStatus.ERROR;
            }

        }
    }
}
