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
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.network.AllocateIpWorker;
import com.godaddy.vps4.web.network.BindIpWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;

import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;

public class ProvisionVmWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ProvisionVmWorker.class);

	final VmService vmService;

	final NetworkService networkService;

	final CreateVmAction action;

	final CountDownLatch inProgressLatch = new CountDownLatch(1);

	final ExecutorService threadPool;

	public ProvisionVmWorker(VmService vmService, NetworkService networkService, CreateVmAction action, ExecutorService threadPool) {
		this.vmService = vmService;
		this.networkService = networkService;
		this.action = action;
		this.threadPool = threadPool;
	}

    @Override
	public void run() {

        logger.info("sending HFS VM request: {}", action.hfsProvisionRequest);

        VmAction hfsAction = vmService.createVm(action.hfsProvisionRequest);

        Future<IpAddress> ipFuture = allocateIp();

        hfsAction = waitForVmAction(hfsAction);

        try {
            IpAddress ip = ipFuture.get();

            new BindIpWorker(networkService, ip.addressId, hfsAction.vmId).run();

            // assert bindAction.status is successful

        } catch (ExecutionException|InterruptedException e) {
            // allocating the IP address failed somehow
            // fail the action
            action.status = ActionStatus.ERROR;
        }

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
            } catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
        }
        return hfsAction;
    }

    protected Future<IpAddress> allocateIp() {
        // while we're waiting on the VM to be created,
        // spin off the IP allocation task

        return threadPool.submit(new AllocateIpWorker(networkService, action.hfsProvisionRequest.sgid));
    }

    public void waitForVmId() {
        while (action.vm == null) {
            try {
                inProgressLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted waiting for action");
            }
        }
    }

}
