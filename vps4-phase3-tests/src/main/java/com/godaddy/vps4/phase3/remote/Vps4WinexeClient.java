package com.godaddy.vps4.phase3.remote;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vps4WinexeClient extends Vps4RemoteAccessClient {
    private static final int MAX_TRIES = 3;

    private static final Logger logger = LoggerFactory.getLogger(Vps4WinexeClient.class);
    private final boolean isWinexeInstalled;

    public Vps4WinexeClient(String primaryIpAddress, String username, String password) {
        super(primaryIpAddress, username, password);

        isWinexeInstalled = existsInPath("winexe");
        if (!isWinexeInstalled && !existsInPath("docker")) {
            throw new RuntimeException("Neither winexe nor docker were found in the program path. "
                    + "At least one of these programs is required to test Windows images.");
        }
    }

    @Override
    public String executeCommand(String powershellCommand) {
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
    public boolean checkConnection() {
        return testWithRetries("cmd.exe /c echo \"testing connection\"",
                               (result) -> result.equals("\"testing connection\""));
    }

    @Override
    public boolean checkHostname(String hostname) {
        String expectedHostname = (hostname.contains("."))
                ? hostname.substring(0, hostname.indexOf("."))
                : hostname;
        return testWithRetries("hostname", (result) -> result.equals(expectedHostname));
    }

    @Override
    public boolean hasAdminPrivilege() {
        String command = "cmd.exe /c net user "+  this.username + " | find /c \"Administrators\"";
        return testWithRetries(command, (result) -> result.equals("1"));
    }

    @Override
    public boolean isActivated() {
        return testWithRetries("cmd.exe /c slmgr /xpr", (result) -> result.contains("Volume activation will expire"));
    }

    @Override
    public boolean hasPanoptaAgent() {
        return testWithRetries("tasklist /FI \"SERVICES eq FortiMonitorAgent\" /FO CSV",
                               (result) -> result.contains("Aggregator.Agent.exe"));
    }

    @Override
    public boolean canPing(String domain) {
        return testWithRetries("ping -n 2 " + domain,
                               (result) -> result.contains("Packets: Sent = 2, Received = 2, Lost = 0 (0% loss)"));
    }

    @Override
    public boolean isRdpRunning() {
        return testWithRetries("cmd.exe /c netstat -an | find /c \"0.0.0.0:3389\"",
                               (result) -> result.equals("2"));
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
    private boolean testWithRetries(String command, Function<String, Boolean> validator) {
        int iterations = 0;
        while (iterations < MAX_TRIES) {
            try {
                String result = executeCommand(command);
                if (validator.apply(result)) {
                    return true;
                }
            } catch (RuntimeException ignored) {}
            iterations++;
        }
        return false;
    }
}
