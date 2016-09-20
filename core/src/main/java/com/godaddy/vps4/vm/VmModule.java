package com.godaddy.vps4.vm;

import javax.inject.Singleton;

import com.godaddy.vps4.hfs.HfsClientProvider;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.jdbc.JdbcControlPanelService;
import com.godaddy.vps4.vm.jdbc.JdbcOsTypeService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.vm.VmService;

public class VmModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
		bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
		bind(ControlPanelService.class).to(JdbcControlPanelService.class);
		bind(OsTypeService.class).to(JdbcOsTypeService.class);
		bind(VmService.class).toProvider(new HfsClientProvider<VmService>(VmService.class)).in(Singleton.class);
	}

}
