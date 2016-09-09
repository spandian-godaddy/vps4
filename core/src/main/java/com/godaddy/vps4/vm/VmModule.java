package com.godaddy.vps4.vm;

import javax.inject.Singleton;

import com.godaddy.vps4.hfs.HfsClientProvider;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.vm.VmService;

public class VmModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
		bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
		bind(VmService.class).toProvider(new HfsClientProvider(VmService.class)).in(Singleton.class);
	}

}
