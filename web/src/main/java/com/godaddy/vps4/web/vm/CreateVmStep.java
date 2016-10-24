package com.godaddy.vps4.web.vm;

public enum CreateVmStep {
    CreatingServer, 
    RequestingIPAddress, 
    GeneratingHostname, 
    RequestingServer, 
    StartingServerSetup, 
    ConfiguringServer, 
    ConfiguringNetwork,
    VerifyingSetup,
    SetupComplete;
}
