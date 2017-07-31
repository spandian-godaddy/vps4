package com.godaddy.vps4.vm;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.godaddy.vps4.vm.jdbc.JdbcControlPanelService;
import com.godaddy.vps4.vm.jdbc.JdbcDataCenterService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmUserService;
import com.google.inject.AbstractModule;

public class VmModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
		bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
		bind(ControlPanelService.class).to(JdbcControlPanelService.class);
        bind(ImageService.class).to(JdbcImageService.class);
        bind(VmUserService.class).to(JdbcVmUserService.class);
		bind(ActionService.class).to(JdbcActionService.class);
		bind(NetworkService.class).to(JdbcNetworkService.class);
		bind(DataCenterService.class).to(JdbcDataCenterService.class);
	}
}
