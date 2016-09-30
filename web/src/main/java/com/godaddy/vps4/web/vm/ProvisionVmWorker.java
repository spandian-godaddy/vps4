package com.godaddy.vps4.web.vm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.network.AllocateIpWorker;
import com.godaddy.vps4.web.network.BindIpWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;

import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;

public class ProvisionVmWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProvisionVmWorker.class);

    final VmService vmService;

    final NetworkService hfsNetworkService;

    final CreateVmAction action;

    final CountDownLatch inProgressLatch = new CountDownLatch(1);

    final ExecutorService threadPool;

    final com.godaddy.vps4.network.NetworkService vps4NetworkService;

    public ProvisionVmWorker(VmService vmService, NetworkService hfsNetworkService, CreateVmAction action, ExecutorService threadPool,
            com.godaddy.vps4.network.NetworkService vps4NetworkService) {
        this.vmService = vmService;
        this.hfsNetworkService = hfsNetworkService;
        this.action = action;
        this.threadPool = threadPool;
        this.vps4NetworkService = vps4NetworkService;
    }

    @Override
    public void run() {

        logger.info("sending HFS VM request: {}", action.hfsProvisionRequest);

        Future<IpAddress> ipFuture = allocateIp();

        IpAddress ip = null;
        try {
            ip = ipFuture.get();
            action.ip = ip;
        }
        catch (ExecutionException | InterruptedException e) {
            // allocating the IP address failed somehow
            // fail the action
            action.status = ActionStatus.ERROR;
        }

        action.hfsProvisionRequest.hostname = HostnameGenerator.getHostname(ip.address);
        VmAction hfsAction = vmService.createVm(action.hfsProvisionRequest);
        hfsAction = waitForVmAction(hfsAction);

        new BindIpWorker(hfsNetworkService, ip.addressId, hfsAction.vmId).run();

        // assert bindAction.status is successful

        logger.info("provisioning complete: {}", hfsAction);

        action.vm = vmService.getVm(hfsAction.vmId);
        action.status = ActionStatus.COMPLETE;
    }

    protected VmAction waitForVmAction(VmAction hfsAction) {
        // wait for VmAction to complete
        while (!hfsAction.state.equals("COMPLETE")) {

            logger.info("waiting on VM to provision: {}", hfsAction);

            if (hfsAction.state.equals("IN_PROGRESS")) {
                action.vm = vmService.getVm(hfsAction.vmId);
                inProgressLatch.countDown();
            }

            // give the VM time to spin up
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
        }
        return hfsAction;
    }

    protected Future<IpAddress> allocateIp() {
        // while we're waiting on the VM to be created,
        // spin off the IP allocation task

        return threadPool.submit(new AllocateIpWorker(hfsNetworkService, action.project, vps4NetworkService));
    }

    public void waitForVmId() {
        while (action.vm == null) {
            try {
                inProgressLatch.await(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted waiting for action");
            }
        }
    }

}
