package com.godaddy.vps4.vm;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.config.ConfigProvider;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.util.VerticalServiceClient;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import gdg.hfs.vhfs.vm.VmService;

public class VmModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Config.class).toProvider(ConfigProvider.class).in(Scopes.SINGLETON);
		bind(PrivilegeService.class).to(JdbcPrivilegeService.class);
		bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
		bind(VmService.class).toInstance(VerticalServiceClient.newClient(VmService.class));
	}
	
}
