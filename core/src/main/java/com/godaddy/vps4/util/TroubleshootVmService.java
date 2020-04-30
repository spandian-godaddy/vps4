package com.godaddy.vps4.util;

public interface TroubleshootVmService {
    String getHfsAgentStatus(long hfsVmId);

    boolean canPingVm(String ipAddress);

    boolean isPortOpenOnVm(String ipAddress, int port);
}
