package com.godaddy.vps4.console;

public interface ConsoleService {
    String getConsoleUrl (long hfsVmId) throws CouldNotRetrieveConsoleException;
}