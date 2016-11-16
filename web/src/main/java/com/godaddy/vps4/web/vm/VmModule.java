package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.ControlPanelService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.OsTypeService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcControlPanelService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcOsTypeService;
import com.godaddy.vps4.vm.jdbc.JdbcVmUserService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.AbstractModule;

public class VmModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
		bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
		bind(ControlPanelService.class).to(JdbcControlPanelService.class);
		bind(OsTypeService.class).to(JdbcOsTypeService.class);
        bind(ImageService.class).to(JdbcImageService.class);
        bind(VmUserService.class).to(JdbcVmUserService.class);
	}
}
