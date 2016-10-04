package com.godaddy.vps4.web.vm;

public enum CreateVmStep {
    CreatingServer, 
    RequestingIPAddress, 
    GeneratingHostname, 
    RequestingServer, 
    StartServerSetup, 
    ConfiguringServer, 
    ConfiguringNetwork, 
    SetupComplete;
}
