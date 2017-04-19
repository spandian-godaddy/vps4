package com.godaddy.vps4.phase3.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.jcraft.jsch.*;

public class Vps4SshClient {

    final Vps4ApiClient vps4ApiClient;
    final String username;
    final String password;

    public Vps4SshClient(Vps4ApiClient vps4ApiClient, String username, String password){
        this.vps4ApiClient = vps4ApiClient;
        this.username = username;
        this.password = password;
    }

    public StringBuffer executeCommand(UUID vmId, String command){
        String primaryIpAddress = getPrimaryIp(vmId);
        try{
            StringBuffer result = new StringBuffer();

            JSch jsch = new JSch();
            Session session=jsch.getSession(this.username, primaryIpAddress, 22);
            session.setPassword(this.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            try {
                ChannelExec channel=(ChannelExec) session.openChannel("exec");
                BufferedReader in=new BufferedReader(new InputStreamReader(channel.getInputStream()));

                channel.setCommand(command);
                channel.connect();
                try {
                    String msg=null;
                    while((msg=in.readLine())!=null){
                      result.append(msg);
                    }
                } finally {
                    channel.disconnect();
                }
            } finally {
                session.disconnect();
            }
            return result;
        }catch(IOException e){
            throw new RuntimeException(e.getMessage(), e.getCause());
        }catch(JSchException e){
            throw new RuntimeException(e.getMessage(), e.getCause());
        }

    }

    public void assertCommandResult(UUID vmId, String expectedResult, String command){
        assert(expectedResult.equals(executeCommand(vmId, command).toString()));
    }

    public boolean checkConnection(UUID vmId){
        String primaryIpAddress = getPrimaryIp(vmId);
        try{
            JSch jsch = new JSch();
            Session session=jsch.getSession(this.username, primaryIpAddress, 22);
            session.setPassword(this.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);
            session.disconnect();
            return true;
        }catch(JSchException e){
            return false;
        }
    }

    private String getPrimaryIp(UUID vmId){
        return vps4ApiClient.getVmPrimaryIp(vmId);
    }
}
