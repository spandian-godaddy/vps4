package com.godaddy.vps4.console;

public interface ConsoleService {
    default String getConsoleUrl (long hfsVmId) throws CouldNotRetrieveConsoleException {
        throw new IllegalArgumentException();
    }

    default String getConsoleUrl (long hfsVmId, String allowedIpAddress) throws CouldNotRetrieveConsoleException {
        throw new IllegalArgumentException();
    };
}