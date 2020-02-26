package com.godaddy.vps4.web;

import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.appmonitors.VmActionsMonitorResource;
import com.godaddy.vps4.web.audit.AuditHfsVmResource;
import com.godaddy.vps4.web.cache.CacheResource;
import com.godaddy.vps4.web.console.ConsoleResource;
import com.godaddy.vps4.web.controlPanel.cpanel.CPanelResource;
import com.godaddy.vps4.web.controlPanel.plesk.PleskResource;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.dns.ActionCommandHelper;
import com.godaddy.vps4.web.dns.DnsResource;
import com.godaddy.vps4.web.dns.ObjectMapperProvider;
import com.godaddy.vps4.web.dns.ReverseDnsLookup;
import com.godaddy.vps4.web.featureFlag.ConfigFeatureMaskModule;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource;
import com.godaddy.vps4.web.messaging.VmMessagingResource;
import com.godaddy.vps4.web.monitoring.PanoptaResource;
import com.godaddy.vps4.web.monitoring.VmAlertResource;
import com.godaddy.vps4.web.monitoring.VmMonitoringResource;
import com.godaddy.vps4.web.monitoring.VmOutageResource;
import com.godaddy.vps4.web.network.NetworkResource;
import com.godaddy.vps4.web.plan.PlanResource;
import com.godaddy.vps4.web.security.Vps4ContainerRequestFilterModule;
import com.godaddy.vps4.web.snapshot.SnapshotActionResource;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.util.TroubleshootVmHelper;
import com.godaddy.vps4.web.validator.ValidatorResource;
import com.godaddy.vps4.web.vm.BackupStorageResource;
import com.godaddy.vps4.web.vm.ImageResource;
import com.godaddy.vps4.web.vm.InventoryResource;
import com.godaddy.vps4.web.vm.OrphanResource;
import com.godaddy.vps4.web.vm.ServerUsageStatsResource;
import com.godaddy.vps4.web.vm.SnapshotScheduleResource;
import com.godaddy.vps4.web.vm.UserResource;
import com.godaddy.vps4.web.vm.VmActionResource;
import com.godaddy.vps4.web.vm.VmDetailsResource;
import com.godaddy.vps4.web.vm.VmFlavorResource;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmRebuildResource;
import com.godaddy.vps4.web.vm.VmRescueResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmRestoreResource;
import com.godaddy.vps4.web.vm.VmShopperMergeResource;
import com.godaddy.vps4.web.vm.VmSnapshotActionResource;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.godaddy.vps4.web.vm.VmSupportUserResource;
import com.godaddy.vps4.web.vm.VmSuspendReinstateResource;
import com.godaddy.vps4.web.vm.VmTroubleshootResource;
import com.godaddy.vps4.web.vm.VmUpgradeResource;
import com.godaddy.vps4.web.vm.VmZombieResource;
import com.google.inject.AbstractModule;

import gdg.hfs.orchestration.web.CommandsResource;
import gdg.hfs.orchestration.web.CommandsViewResource;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {
        bind(StatusResource.class);

        bind(VmResource.class);
        bind(VmRestoreResource.class);
        bind(VmRebuildResource.class);
        bind(VmActionResource.class);
        bind(VmFlavorResource.class);
        bind(VmPatchResource.class);
        bind(VmMailRelayResource.class);
        bind(VmMonitoringResource.class);
        bind(VmSupportUserResource.class);
        bind(VmZombieResource.class);
        bind(VmTroubleshootResource.class);
        bind(TroubleshootVmHelper.class);
        bind(ValidatorResource.class);
        bind(CPanelResource.class);
        bind(PleskResource.class);
        bind(SysAdminResource.class);
        bind(UserResource.class);
        bind(CreditResource.class);
        bind(ImageResource.class);
        bind(ServerUsageStatsResource.class);
        bind(CacheResource.class);
        bind(SnapshotResource.class);
        bind(VmSnapshotResource.class);
        bind(SnapshotScheduleResource.class);
        bind(NetworkResource.class);
        bind(SnapshotActionResource.class);
        bind(VmSnapshotActionResource.class);
        bind(Vps4ExceptionMapper.class);
        bind(VmActionsMonitorResource.class);
        bind(ConsoleResource.class);
        bind(VmMessagingResource.class);
        bind(VmUpgradeResource.class);
        bind(ActionResource.class);
        bind(VmSuspendReinstateResource.class);
        bind(OrphanResource.class);
        bind(PlanResource.class);
        bind(AuditHfsVmResource.class);
        bind(VmRescueResource.class);
        bind(VmShopperMergeResource.class);
        bind(ObjectMapperProvider.class); // do not remove - Guice requires explicit binding
        bind(ActionCommandHelper.class);
        bind(ReverseDnsLookup.class);
        bind(DnsResource.class);
        bind(PanoptaResource.class);
        bind(VmAlertResource.class);
        bind(VmOutageResource.class);
        bind(VmDetailsResource.class);
        bind(InventoryResource.class);
        bind(BackupStorageResource.class);

        bind(CommandsResource.class);
        bind(CommandsViewResource.class);

        install(new ActionCancelModule());
        install(new Vps4ContainerRequestFilterModule());
        install(new ConfigFeatureMaskModule());
    }
}
