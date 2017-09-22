package com.godaddy.vps4.vm;

public enum RestoreVmStep {
    UnbindingIPAddress,
    RequestingServer,
    ConfiguringNetwork,
    DeleteOldVm;
}
