package com.godaddy.vps4.orchestration.hfs;

import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.WaitForCpanelAction;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.network.WaitForAddressAction;
import com.godaddy.vps4.orchestration.hfs.pingcheck.CreateCheck;
import com.godaddy.vps4.orchestration.hfs.pingcheck.DeleteCheck;
import com.godaddy.vps4.orchestration.hfs.pingcheck.WaitForPingCheckAction;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.UpdateAdminPassword;
import com.godaddy.vps4.orchestration.hfs.plesk.WaitForPleskAction;
import com.godaddy.vps4.orchestration.hfs.sysadmin.RefreshCpanelLicense;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.hfs.vm.RestartVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.vm.WaitForManageVmAction;
import com.google.inject.AbstractModule;

public class HfsCommandModule extends AbstractModule {

    @Override
    public void configure() {

        // VM
        bind(CreateVm.class);
        bind(DestroyVm.class);
        bind(RestartVm.class);
        bind(StartVm.class);
        bind(StopVm.class);
        bind(WaitForVmAction.class);
        bind(WaitForManageVmAction.class);

        // Network
        bind(AllocateIp.class);
        bind(BindIp.class);
        bind(ReleaseIp.class);
        bind(UnbindIp.class);
        bind(WaitForAddressAction.class);

        // SysAdmin
        bind(SetPassword.class);
        bind(ToggleAdmin.class);
        bind(SetHostname.class);
        bind(WaitForSysAdminAction.class);
        bind(ConfigureMailRelay.class);

        // CPanel
        bind(ConfigureCpanel.class);
        bind(RefreshCpanelLicense.class);        
        bind(WaitForCpanelAction.class);

        // Plesk
        bind(ConfigurePlesk.class);
        bind(UpdateAdminPassword.class);
        bind(WaitForPleskAction.class);

        // PingCheck
        bind(CreateCheck.class);
        bind(DeleteCheck.class);
        bind(WaitForPingCheckAction.class);
    }
}
