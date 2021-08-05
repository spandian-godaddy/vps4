package com.godaddy.vps4.vm;

import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.ReplicationLagService;
import com.godaddy.vps4.appmonitors.jdbc.JdbcMonitorService;
import com.godaddy.vps4.appmonitors.jdbc.JdbcReplicationLagService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduledJob.jdbc.JdbcScheduledJobService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.jdbc.JdbcControlPanelService;
import com.godaddy.vps4.vm.jdbc.JdbcDataCenterService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;
import com.godaddy.vps4.vm.jdbc.JdbcVmAlertService;
import com.godaddy.vps4.vm.jdbc.JdbcVmOutageService;
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
        bind(ActionService.class).to(JdbcVmActionService.class);
        bind(NetworkService.class).to(JdbcNetworkService.class);
        bind(DataCenterService.class).to(JdbcDataCenterService.class);
        bind(ScheduledJobService.class).to(JdbcScheduledJobService.class);
        bind(MonitorService.class).to(JdbcMonitorService.class);
        bind(VmAlertService.class).to(JdbcVmAlertService.class);
        bind(VmOutageService.class).to(JdbcVmOutageService.class);
        bind(ReplicationLagService.class).to(JdbcReplicationLagService.class);
    }
}
