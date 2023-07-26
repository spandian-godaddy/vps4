package com.godaddy.vps4.orchestration;

import com.godaddy.vps4.orchestration.console.Vps4RequestConsole;
import com.godaddy.vps4.orchestration.cpanel.InstallPackage;
import com.godaddy.vps4.orchestration.cpanel.Vps4InstallCPanelPackage;
import com.godaddy.vps4.orchestration.cpanel.WaitForPackageInstall;
import com.godaddy.vps4.orchestration.cpanel.Vps4AddAddOnDomain;
import com.godaddy.vps4.orchestration.cpanel.Vps4ValidateDomainConfig;
import com.godaddy.vps4.orchestration.dns.Vps4CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanoptaAgent;
import com.godaddy.vps4.orchestration.hfs.sysadmin.UninstallPanoptaAgent;
import com.godaddy.vps4.orchestration.mailrelay.Vps4SetMailRelayQuota;
import com.godaddy.vps4.orchestration.messaging.SendFailoverCompletedEmail;
import com.godaddy.vps4.orchestration.messaging.SendScheduledPatchingEmail;
import com.godaddy.vps4.orchestration.messaging.SendSetupCompletedEmail;
import com.godaddy.vps4.orchestration.messaging.SendSystemDownFailoverEmail;
import com.godaddy.vps4.orchestration.messaging.SendUnexpectedButScheduledMaintenanceEmail;
import com.godaddy.vps4.orchestration.monitoring.ClearJsdOutageTicket;
import com.godaddy.vps4.orchestration.monitoring.GetPanoptaOutage;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.orchestration.monitoring.SendVmOutageEmail;
import com.godaddy.vps4.orchestration.monitoring.SendVmOutageResolvedEmail;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddDomainMonitoring;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddMonitoring;
import com.godaddy.vps4.orchestration.monitoring.Vps4ClearVmOutage;
import com.godaddy.vps4.orchestration.monitoring.Vps4NewVmOutage;
import com.godaddy.vps4.orchestration.network.RemoveIpFromBlacklist;
import com.godaddy.vps4.orchestration.panopta.AddAdditionalFqdnPanopta;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.panopta.Vps4RemoveDomainMonitoring;
import com.godaddy.vps4.orchestration.panopta.Vps4ReplaceDomainMonitoring;
import com.godaddy.vps4.orchestration.panopta.WaitForPanoptaAgentSync;
import com.godaddy.vps4.orchestration.snapshot.Vps4DeprecateSnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.orchestration.sysadmin.Vps4AddSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4EnableWinexe;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetHostname;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetCustomerPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetSupportUserPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4ToggleAdmin;
import com.godaddy.vps4.orchestration.monitoring.CreateJsdOutageTicket;
import com.godaddy.vps4.orchestration.vm.UnlicenseControlPanel;
import com.godaddy.vps4.orchestration.vm.Vps4AddIpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4CancelAction;
import com.godaddy.vps4.orchestration.vm.Vps4DeleteAllScheduledJobsForVm;
import com.godaddy.vps4.orchestration.vm.Vps4DeleteAllScheduledZombieJobsForVm;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyDedicated;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyIpAddressAction;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyOHVm;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4EndRescue;
import com.godaddy.vps4.orchestration.vm.Vps4MoveOut;
import com.godaddy.vps4.orchestration.vm.Vps4PlanChange;
import com.godaddy.vps4.orchestration.vm.Vps4ProcessReinstateServer;
import com.godaddy.vps4.orchestration.vm.Vps4ProcessSuspendServer;
import com.godaddy.vps4.orchestration.vm.Vps4RebootDedicated;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.orchestration.vm.Vps4RemoveIp;
import com.godaddy.vps4.orchestration.vm.Vps4RemoveSupportUsersFromDatabase;
import com.godaddy.vps4.orchestration.vm.Vps4Rescue;
import com.godaddy.vps4.orchestration.vm.Vps4RestartVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestoreOHVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestoreVm;
import com.godaddy.vps4.orchestration.vm.Vps4ReviveZombieVm;
import com.godaddy.vps4.orchestration.vm.Vps4StartVm;
import com.godaddy.vps4.orchestration.vm.Vps4StopVm;
import com.godaddy.vps4.orchestration.vm.Vps4SubmitReinstateServer;
import com.godaddy.vps4.orchestration.vm.Vps4SubmitSuspendServer;
import com.godaddy.vps4.orchestration.vm.Vps4SyncVmStatus;
import com.godaddy.vps4.orchestration.vm.Vps4TestCommand;
import com.godaddy.vps4.orchestration.vm.Vps4UnclaimCredit;
import com.godaddy.vps4.orchestration.vm.Vps4UpgradeOHVm;
import com.godaddy.vps4.orchestration.vm.Vps4UpgradeVm;
import com.godaddy.vps4.orchestration.vm.provision.Vps4ProvisionDedicated;
import com.godaddy.vps4.orchestration.vm.provision.Vps4ProvisionOHVm;
import com.godaddy.vps4.orchestration.vm.provision.Vps4ProvisionVm;
import com.godaddy.vps4.orchestration.vm.rebuild.Vps4RebuildDedicated;
import com.godaddy.vps4.orchestration.vm.rebuild.Vps4RebuildOHVm;
import com.godaddy.vps4.orchestration.vm.rebuild.Vps4RebuildVm;
import com.google.inject.AbstractModule;

public class Vps4CommandModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Vps4ProvisionVm.class);
        bind(Vps4ProvisionDedicated.class);
        bind(Vps4ProvisionOHVm.class);
        bind(Vps4ToggleAdmin.class);
        bind(Vps4SetCustomerPassword.class);
        bind(Vps4SetSupportUserPassword.class);
        bind(Vps4SetHostname.class);
        bind(Vps4TestCommand.class);
        bind(Vps4DestroyVm.class);
        bind(Vps4DestroyDedicated.class);
        bind(Vps4DestroyOHVm.class);
        bind(Vps4StartVm.class);
        bind(Vps4StopVm.class);
        bind(Vps4RestartVm.class);
        bind(Vps4RebootDedicated.class);
        bind(Vps4RemoveIp.class);
        bind(Vps4DestroyIpAddressAction.class);
        bind(Vps4AddIpAddress.class);
        bind(Vps4AddSupportUser.class);
        bind(Vps4RemoveSupportUser.class);
        bind(Vps4SnapshotVm.class);
        bind(Vps4DeprecateSnapshot.class);
        bind(Vps4DestroySnapshot.class);
        bind(Vps4SetMailRelayQuota.class);
        bind(Vps4RestoreVm.class);
        bind(Vps4RestoreOHVm.class);
        bind(Vps4RebuildVm.class);
        bind(Vps4RebuildDedicated.class);
        bind(Vps4RebuildOHVm.class);
        bind(Vps4PlanChange.class);
        bind(Vps4ReviveZombieVm.class);
        bind(Vps4DeleteAllScheduledJobsForVm.class);
        bind(Vps4DeleteAllScheduledZombieJobsForVm.class);
        bind(Vps4RecordScheduledJobForVm.class);
        bind(Vps4RemoveSupportUsersFromDatabase.class);
        bind(UnlicenseControlPanel.class);
        bind(SendScheduledPatchingEmail.class);
        bind(SendUnexpectedButScheduledMaintenanceEmail.class);
        bind(SendSystemDownFailoverEmail.class);
        bind(SendFailoverCompletedEmail.class);
        bind(SendSetupCompletedEmail.class);
        bind(SendVmOutageEmail.class);
        bind(SendVmOutageResolvedEmail.class);
        bind(Vps4CancelAction.class);
        bind(Vps4UpgradeVm.class);
        bind(Vps4UpgradeOHVm.class);
        bind(Vps4EndRescue.class);
        bind(Vps4Rescue.class);
        bind(Vps4CreateDnsPtrRecord.class);
        bind(PausePanoptaMonitoring.class);
        bind(ResumePanoptaMonitoring.class);
        bind(Vps4AddMonitoring.class);
        bind(RemovePanoptaMonitoring.class);
        bind(SetupPanopta.class);
        bind(InstallPanoptaAgent.class);
        bind(UninstallPanoptaAgent.class);
        bind(WaitForPanoptaAgentSync.class);
        bind(Vps4RequestConsole.class);
        bind(Vps4SyncVmStatus.class);
        bind(RemoveIpFromBlacklist.class);
        bind(Vps4EnableWinexe.class);
        bind(Vps4SubmitSuspendServer.class);
        bind(Vps4SubmitReinstateServer.class);
        bind(Vps4ProcessSuspendServer.class);
        bind(Vps4ProcessReinstateServer.class);
        bind(Vps4AddDomainMonitoring.class);
        bind(AddAdditionalFqdnPanopta.class);
        bind(Vps4UnclaimCredit.class);
        bind(Vps4RemoveDomainMonitoring.class);
        bind(Vps4ReplaceDomainMonitoring.class);
        bind(Vps4NewVmOutage.class);
        bind(Vps4ClearVmOutage.class);
        bind(GetPanoptaOutage.class);
        bind(CreateJsdOutageTicket.class);
        bind(ClearJsdOutageTicket.class);
        bind(Vps4InstallCPanelPackage.class);
        bind(InstallPackage.class);
        bind(WaitForPackageInstall.class);
        bind(Vps4AddAddOnDomain.class);
        bind(Vps4ValidateDomainConfig.class);
        bind(Vps4MoveOut.class);
    }
}
