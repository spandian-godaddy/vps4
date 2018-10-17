package com.godaddy.vps4.vm;

public enum RebuildVmStep {
    UnbindingIPAddress,
    RequestingServer,
    ConfiguringNetwork,
    CreatingServer,
    RequestingIPAddress,
    RequestingMailRelay,
    GeneratingHostname,
    StartingServerSetup,
    ConfiguringServer,
    SettingAdminAccess,
    VerifyingSetup,
    ConfiguringCPanel,
    ConfiguringPlesk,
    ConfigureMailRelay,
    SetHostname,
    SetupAutomaticBackupSchedule,
    RebuildComplete,
    DeleteOldVm
}
