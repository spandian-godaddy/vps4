package com.godaddy.vps4.orchestration;

import com.godaddy.vps4.orchestration.mailrelay.Vps4SetMailRelayQuota;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.orchestration.sysadmin.Vps4AddSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetHostname;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4ToggleAdmin;
import com.godaddy.vps4.orchestration.vm.Vps4AddIpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyIpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyIpAddressAction;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4PlanChange;
import com.godaddy.vps4.orchestration.vm.Vps4ProvisionVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestartVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestoreVm;
import com.godaddy.vps4.orchestration.vm.Vps4ReviveZombieVm;
import com.godaddy.vps4.orchestration.vm.Vps4StartVm;
import com.godaddy.vps4.orchestration.vm.Vps4StopVm;
import com.godaddy.vps4.orchestration.vm.Vps4TestCommand;
import com.google.inject.AbstractModule;

public class Vps4CommandModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Vps4ProvisionVm.class);
        bind(Vps4ToggleAdmin.class);
        bind(Vps4SetPassword.class);
        bind(Vps4SetHostname.class);
        bind(Vps4TestCommand.class);
        bind(Vps4DestroyVm.class);
        bind(Vps4StartVm.class);
        bind(Vps4StopVm.class);
        bind(Vps4RestartVm.class);
        bind(Vps4DestroyIpAddress.class);
        bind(Vps4DestroyIpAddressAction.class);
        bind(Vps4AddIpAddress.class);
        bind(Vps4AddSupportUser.class);
        bind(Vps4RemoveUser.class);
        bind(Vps4SnapshotVm.class);
        bind(Vps4DestroySnapshot.class);
        bind(Vps4SetMailRelayQuota.class);
        bind(Vps4RestoreVm.class);
        bind(Vps4PlanChange.class);
        bind(Vps4ReviveZombieVm.class);
    }
}
