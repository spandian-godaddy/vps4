package com.godaddy.vps4.orchestration;

import com.godaddy.vps4.orchestration.sysadmin.Vps4SetHostname;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4ToggleAdmin;
import com.godaddy.vps4.orchestration.vm.ProvisionVm;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyIpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestartVm;
import com.godaddy.vps4.orchestration.vm.Vps4StartVm;
import com.godaddy.vps4.orchestration.vm.Vps4StopVm;
import com.godaddy.vps4.orchestration.vm.Vps4TestCommand;
import com.google.inject.AbstractModule;

public class Vps4CommandModule extends AbstractModule {

    @Override
    public void configure() {
        bind(ProvisionVm.class);
        bind(Vps4ToggleAdmin.class);
        bind(Vps4SetPassword.class);
        bind(Vps4SetHostname.class);
        bind(Vps4TestCommand.class);
        bind(Vps4DestroyVm.class);
        bind(Vps4StartVm.class);
        bind(Vps4StopVm.class);
        bind(Vps4RestartVm.class);
        bind(Vps4DestroyIpAddress.class);
    }
}
