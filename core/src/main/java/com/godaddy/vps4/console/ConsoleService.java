package com.godaddy.vps4.console;

@Deprecated // this will no longer be needed once the old /console endpoint is removed
public interface ConsoleService {
    default String getConsoleUrl (long hfsVmId) throws CouldNotRetrieveConsoleException {
        throw new IllegalArgumentException();
    }

    default String getConsoleUrl (long hfsVmId, String allowedIpAddress) throws CouldNotRetrieveConsoleException {
        throw new IllegalArgumentException();
    };
}