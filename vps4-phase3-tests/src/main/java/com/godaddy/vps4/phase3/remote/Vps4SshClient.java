package com.godaddy.vps4.phase3.remote;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Vps4SshClient extends Vps4RemoteAccessClient {

    private static final Logger logger = LoggerFactory.getLogger(Vps4SshClient.class);

    public Vps4SshClient(String primaryIpAddress, String username, String password) {
        super(primaryIpAddress, username, password);
    }

    @Override
    public String executeCommand(String command) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(this.username, primaryIpAddress, 22);
            session.setPassword(this.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            String result;
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            try {
                channel.setCommand(command);
                channel.connect();
                try {
                    result = streamToString(channel.getInputStream());
                } finally {
                    channel.disconnect();
                }
            } finally {
                session.disconnect();
            }
            return result;
        } catch (IOException | JSchException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public boolean checkConnection() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(this.username, primaryIpAddress, 22);
            session.setPassword(this.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            session.disconnect();
            return true;
        } catch (JSchException e) {
            logger.info("Exception checking ssh connection: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkHostname(String expectedHostname) {
        String result = executeCommand("hostname");
        logger.info("check hostname result: {}", result);
        return expectedHostname.equals(result);
    }

    @Override
    public boolean hasAdminPrivilege() {
        String result = executeCommand("sudo -n id");
        logger.info("'sudo -n id' result: {}", result);
        return result.contains("uid=0(root) gid=0(root) groups=0(root)");
    }

    @Override
    public boolean isActivated() {
        // Linux VMs do not require activation
        return false;
    }

    @Override
    public boolean hasPanoptaAgent() {
        String result = executeCommand("test -d /etc/panopta-agent && echo \"success\" || " +
                                               "test -d /etc/fm-agent && echo \"success\" || echo \"failure\"");
        return result.equals("success");
    }

    @Override
    public boolean canPing(String domain) {
        String result = executeCommand("ping -c 2 " + domain);
        return result.contains("2 packets transmitted, 2 received, 0% packet loss");
    }

    @Override
    public boolean isRdpRunning() {
        // Linux VMs do not use RDP
        return false;
    }
}
