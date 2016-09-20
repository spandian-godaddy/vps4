package com.godaddy.vps4.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.hfs.CreateVMRequest;
import com.godaddy.vps4.hfs.Flavor;
import com.godaddy.vps4.hfs.Vm;
import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.hfs.VmService.FlavorList;
import com.godaddy.vps4.web.VmsResource.ActionStatus;
import com.godaddy.vps4.web.VmsResource.CreateVmAction;

public class CreateVmWorker implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CreateVmWorker.class);

	final VmService vmService;
	
	final CreateVmAction action;
	
	public CreateVmWorker(VmService vmService, CreateVmAction action) {
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
		logger.info("finding HFS flavor details: {}", action.flavor);
		Flavor flavor = findFlavor(action.flavor);
		if (flavor == null ) {
			action.status = ActionStatus.ERROR;
			action.message = "Unknown VM flavor: " + action.flavor;
			return;
		}
		
		CreateVMRequest hfsCreateRequest = new CreateVMRequest();
		hfsCreateRequest.cpuCores = (int) flavor.cpuCores;
		hfsCreateRequest.diskGiB = (int) flavor.diskGiB;
		hfsCreateRequest.ramMiB = (int) flavor.ramMiB;
		
		hfsCreateRequest.sgid = action.hfsSgid;
		hfsCreateRequest.image_name = action.image;
		hfsCreateRequest.os = action.image;
		
		hfsCreateRequest.hostname = action.hostname;
		
		hfsCreateRequest.username = action.username;
		hfsCreateRequest.password = action.password;
		
		logger.info("sending HFS VM request: {}", hfsCreateRequest);
		
		VmAction hfsAction = vmService.createVm(hfsCreateRequest);
		
		// TODO verify hfsAction created successfully
		
		// wait for VmAction to complete
		while (!hfsAction.state.equals("RUNNING")) {
			
			logger.info("waiting on VM to provision: {}", hfsAction);
			
			// give the VM time to spin up
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.warn("Interrupted while sleeping");
			}
			
			hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
		}
		
		// 
		action.vmId = hfsAction.vmId;
		
		Vm vm = vmService.getVm(action.vmId);
		// assert vm != null
		
		action.ip = vm.address.ip_address;
		action.status = ActionStatus.COMPLETE;
	}
	
}
