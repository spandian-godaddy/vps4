package com.godaddy.vps4.vm;

public enum CreateVmStep {
    CreatingServer, 
    RequestingIPAddress, 
    RequestingMailRelay,
    GeneratingHostname, 
    RequestingServer, 
    StartingServerSetup, 
    ConfiguringServer, 
    ConfiguringNetwork,
    SettingAdminAccess,
    VerifyingSetup,
    SetupComplete,
    ConfiguringCPanel,
    ConfiguringPlesk,
    SetHostname;
}
