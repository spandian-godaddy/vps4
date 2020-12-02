package com.godaddy.vps4.phase3.remote;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;

public class Vps4WinexeClient extends Vps4RemoteAccessClient {
    private static final int MAX_TRIES = 5;

    private static final Logger logger = LoggerFactory.getLogger(Vps4WinexeClient.class);
    private final boolean isWinexeInstalled;

    public Vps4WinexeClient(Vps4ApiClient vps4ApiClient, String username, String password) {
        super(vps4ApiClient, username, password);

        isWinexeInstalled = existsInPath("winexe");
        if (!isWinexeInstalled && !existsInPath("docker")) {
            throw new RuntimeException("Neither winexe nor docker were found in the program path. "
                    + "At least one of these programs is required to test Windows images.");
        }
    }

    @Override
    public String executeCommand(UUID vmId, String powershellCommand) {
        String primaryIpAddress = vps4ApiClient.getVmPrimaryIp(vmId);

        String winexeCommand = String.format(
                "winexe --reinstall -U '%s'%%'%s' //%s '%s'",
                escapeSingleQuotes(username),
                escapeSingleQuotes(password),
                primaryIpAddress,
                escapeSingleQuotes(powershellCommand)
        );
        String dockerCommand = String.format(
                "docker run -i --rm winexe:1.1 %s",
                winexeCommand
        );
        String processCommand = (isWinexeInstalled) ? winexeCommand : dockerCommand;
        logger.debug("running winexe with the following command:\n{}", processCommand);

        try {
            Process process = Runtime.getRuntime().exec(new String[] {
                    "sh",
                    "-c",
                    processCommand
            });
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("Winexe command timed out and was destroyed");
            }

            String result;
            try (InputStream stream = process.getInputStream()) {
                result = streamToString(stream);
                logger.debug("winexe input stream:\n{}", result);
            }
            try (InputStream stream = process.getErrorStream()) {
                String err = streamToString(stream);
                logger.debug("winexe error stream:\n{}", err);
            }
            return result;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public boolean checkConnection(UUID vmId) {
        return testWithRetries(vmId, "cmd.exe /c echo \"testing connection\"", "\"testing connection\"");
    }

    @Override
    public boolean checkHostname(UUID vmId, String expectedHostname) {
        if (expectedHostname.contains(".")) {
            expectedHostname = expectedHostname.substring(0, expectedHostname.indexOf("."));
        }
        return testWithRetries(vmId, "hostname", expectedHostname);
    }

    private String escapeSingleQuotes(String s) {
        return s.replace("'", "\\'");
    }

    private boolean existsInPath(String programName) {
        return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
                     .map(Paths::get)
                     .anyMatch(path -> Files.exists(path.resolve(programName)));
    }

    /*
     * Sometimes Winexe will freeze, timeout or throw errors even though the VM is working fine.
     */
    private boolean testWithRetries(UUID vmId, String command, String expected) {
        int iterations = 0;
        while (iterations < MAX_TRIES) {
            try {
                String result = executeCommand(vmId, command);
                if (result.equals(expected)) {
                    return true;
                }
            } catch (RuntimeException ignored) {}
            iterations++;
        }
        return false;
    }
}
