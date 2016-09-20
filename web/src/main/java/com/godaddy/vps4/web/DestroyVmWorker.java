package com.godaddy.vps4.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.web.VmsResource.ActionStatus;
import com.godaddy.vps4.web.VmsResource.DestroyVmAction;

public class DestroyVmWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(DestroyVmWorker.class);
	
	final VmService vmService;
	
	final DestroyVmAction action;
	
	public DestroyVmWorker(VmService vmService, DestroyVmAction action) {
		this.vmService = vmService;
		this.action = action;
	}
		
	
	@Override
	public void run() {
		
		logger.info("destroying VM {}", action.vmId);
		
		VmAction hfsAction = vmService.destroyVm(action.vmId);
		
		// wait for the HFS action to complete
		while (hfsAction.state.equals("IN_PROGRESS")) {
			logger.info("waiting on VM to be destroyed: {}", hfsAction);
			
			// give the VM time to spin up
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.warn("Interrupted while sleeping");
			}
			
			hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
		}
		
		logger.info("VM destroyed: {}", hfsAction);
		
		// TODO check for HFS action error
		
		action.status = ActionStatus.COMPLETE;
	}
}
