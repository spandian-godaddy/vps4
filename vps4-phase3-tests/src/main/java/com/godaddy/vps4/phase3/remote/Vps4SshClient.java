package com.godaddy.vps4.phase3.remote;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Vps4SshClient extends Vps4RemoteAccessClient {

    private static final Logger logger = LoggerFactory.getLogger(Vps4SshClient.class);

    public Vps4SshClient(Vps4ApiClient vps4ApiClient, String username, String password) {
        super(vps4ApiClient, username, password);
    }

    @Override
    public String executeCommand(UUID vmId, String command) {
        String primaryIpAddress = vps4ApiClient.getVmPrimaryIp(vmId);
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
    public boolean checkConnection(UUID vmId) {
        String primaryIpAddress = vps4ApiClient.getVmPrimaryIp(vmId);
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(this.username, primaryIpAddress, 22);
            session.setPassword(this.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            session.disconnect();
            return true;
        } catch (JSchException e) {
            logger.error("Exception checking ssh connection: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkHostname(UUID vmId, String expectedHostname) {
        String result = executeCommand(vmId, "hostname");
        logger.info("check hostname result: {}", result);
        return expectedHostname.equals(result);
    }
}
