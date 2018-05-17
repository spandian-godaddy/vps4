package com.godaddy.vps4.web.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TroubleshootVmHelper {

    private static final Logger logger = LoggerFactory.getLogger(TroubleshootVmHelper.class);
    private final int ONE_SECOND = 1000;

    public boolean canPingVm(String ipAddress) {
        try {
            InetAddress inetAddr = getInetAddress(ipAddress);
            return inetAddr.isReachable(ONE_SECOND);
        } catch (IOException ex) {
            logger.info("Unable to ping VM IP : {}", ipAddress, ex);
        }

        return false;
    }

    public boolean isPortOpenOnVm(String ipAddress, int port) {
        Socket s = this.createSocket();
        try {
            InetAddress inetAddr = this.getInetAddress(ipAddress);
            InetSocketAddress socketAddress = new InetSocketAddress(inetAddr, port);
            s.connect(socketAddress, ONE_SECOND);;
            return true;
        } catch (IOException ex) {
            logger.info("Port {}:{} is not available", ipAddress, port, ex);
        } finally {
            try {
                s.close();
            } catch (IOException ex) {
                logger.error("Unable to close test socket! Address {}:{}", ipAddress, port, ex);
            }
        }
        return false;
    }

    public Socket createSocket() {
        return new Socket();
    }

    public InetAddress getInetAddress(String ipAddress) throws IOException {
        return InetAddress.getByName(ipAddress);
    }
}
