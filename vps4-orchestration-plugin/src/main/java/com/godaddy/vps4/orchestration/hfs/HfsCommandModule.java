package com.godaddy.vps4.orchestration.hfs;

import com.godaddy.vps4.orchestration.dns.WaitForDnsAction;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.RefreshCpanelLicense;
import com.godaddy.vps4.orchestration.hfs.cpanel.UnlicenseCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.WaitForCpanelAction;
import com.godaddy.vps4.orchestration.hfs.dns.CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.network.WaitForAddressAction;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.SetPleskOutgoingEmailIp;
import com.godaddy.vps4.orchestration.hfs.plesk.UnlicensePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.UpdateAdminPassword;
import com.godaddy.vps4.orchestration.hfs.plesk.WaitForPleskAction;
import com.godaddy.vps4.orchestration.hfs.snapshot.DestroySnapshot;
import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.RemoveUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVmFromSnapshot;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildVm;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.RestartVm;
import com.godaddy.vps4.orchestration.hfs.vm.RestoreOHVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.hfs.vm.ResizeOHVm;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.monitoring.HandleMonitoringDownEvent;
import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.orchestration.vm.WaitForManageVmAction;
import com.google.inject.AbstractModule;

public class HfsCommandModule extends AbstractModule {

    @Override
    public void configure() {

        // VM
        bind(CreateVm.class);
        bind(CreateVmFromSnapshot.class);
        bind(DestroyVm.class);
        bind(RestartVm.class);
        bind(StartVm.class);
        bind(StopVm.class);
        bind(RescueVm.class);
        bind(EndRescueVm.class);
        bind(RebuildVm.class);
        bind(WaitForVmAction.class);
        bind(WaitForManageVmAction.class);
        bind(WaitForAndRecordVmAction.class);
        bind(RestoreOHVm.class);
        bind(ResizeOHVm.class);

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
        bind(AddUser.class);
        bind(RemoveUser.class);

        // CPanel
        bind(ConfigureCpanel.class);
        bind(RefreshCpanelLicense.class);
        bind(WaitForCpanelAction.class);
        bind(UnlicenseCpanel.class);

        // Plesk
        bind(ConfigurePlesk.class);
        bind(UpdateAdminPassword.class);
        bind(WaitForPleskAction.class);
        bind(UnlicensePlesk.class);
        bind(SetPleskOutgoingEmailIp.class);

        // Snapshot
        bind(WaitForSnapshotAction.class);
        bind(DestroySnapshot.class);

        // MailRelay
        bind(SetMailRelayQuota.class);

        // Monitoring
        bind(HandleMonitoringDownEvent.class);

        // dns
        bind(CreateDnsPtrRecord.class);
        bind(WaitForDnsAction.class);
    }
}
