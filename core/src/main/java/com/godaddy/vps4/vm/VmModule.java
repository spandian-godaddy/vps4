package com.godaddy.vps4.vm;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.vm.jdbc.*;
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
		bind(ActionService.class).to(JdbcActionService.class);
	}
}
