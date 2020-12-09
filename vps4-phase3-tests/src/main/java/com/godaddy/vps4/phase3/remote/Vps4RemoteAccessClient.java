package com.godaddy.vps4.phase3.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;

public abstract class Vps4RemoteAccessClient {
    final Vps4ApiClient vps4ApiClient;
    final String username;
    final String password;

    public Vps4RemoteAccessClient(Vps4ApiClient vps4ApiClient, String username, String password) {
        this.vps4ApiClient = vps4ApiClient;
        this.username = username;
        this.password = password;
    }

    public abstract String executeCommand(UUID vmId, String command);

    public abstract boolean checkConnection(UUID vmId);

    public abstract boolean checkHostname(UUID vmId, String expectedHostname);

    public abstract boolean hasAdminPrivilege(UUID vmId);

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
