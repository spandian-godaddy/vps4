package com.godaddy.vps4.web.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.hfs.Flavor;
import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.hfs.VmService.FlavorList;
import com.godaddy.vps4.web.vm.VmsResource.ActionStatus;
import com.godaddy.vps4.web.vm.VmsResource.CreateVmAction;

public class ProvisionVmWorker implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ProvisionVmWorker.class);

	final VmService vmService;
	
	final CreateVmAction action;
	
	public ProvisionVmWorker(VmService vmService, CreateVmAction action) {
		this.vmService = vmService;
		this.action = action;
	}
	
	protected Flavor findFlavor(String name) {
		
		FlavorList flavors = vmService.getFlavors();
		if (flavors != null && flavors.results != null) {
			for (Flavor flavor : flavors.results) {
				if (flavor.name.equals(name)) {
					return flavor;
				}
			}
		}
		return null;
	}
	
	@Override
	public void run() {
		
        logger.info("sending HFS VM request: {}", action.hfsProvisionRequest);
		
        VmAction hfsAction = vmService.createVm(action.hfsProvisionRequest);
		
		// wait for VmAction to complete
		while (!hfsAction.state.equals("COMPLETE")) {
			
			logger.info("waiting on VM to provision: {}", hfsAction);
			
			if (hfsAction.state.equals("IN_PROGRESS")) {
				action.vm = vmService.getVm(hfsAction.vmId);
				synchronized (this) {
				    this.notify();
				}
			}
			
			// give the VM time to spin up
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.warn("Interrupted while sleeping");
			}
			
			hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
		}
		
        logger.info("provisioning complete: {}", hfsAction);

		action.vm = vmService.getVm(hfsAction.vmId);		
		action.status = ActionStatus.COMPLETE;
	}
	
}
