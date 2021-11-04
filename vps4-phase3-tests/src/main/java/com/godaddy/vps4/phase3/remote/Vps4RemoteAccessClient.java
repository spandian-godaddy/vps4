package com.godaddy.vps4.phase3.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class Vps4RemoteAccessClient {
    final String primaryIpAddress;
    final String username;
    final String password;

    public Vps4RemoteAccessClient(String primaryIpAddress, String username, String password) {
        this.primaryIpAddress = primaryIpAddress;
        this.username = username;
        this.password = password;
    }

    public abstract String executeCommand(String command);

    public abstract boolean checkConnection();

    public abstract boolean checkHostname(String expectedHostname);

    public abstract boolean hasAdminPrivilege();

    public abstract boolean isActivated();

    String streamToString(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        }
        return result
                .toString()
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .trim();
    }
}
