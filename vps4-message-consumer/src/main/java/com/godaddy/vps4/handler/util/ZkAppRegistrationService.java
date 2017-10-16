package com.godaddy.vps4.handler.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.godaddy.hfs.io.Charsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkAppRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(ZkAppRegistrationService.class);

    private final String serviceName;

    private final String basePath;

    private final CuratorFramework curator;

    private final UUID id;

    public ZkAppRegistrationService(String basePath, String serviceName, CuratorFramework curator) {
        this.basePath = basePath;
        this.serviceName = serviceName;
        this.curator = curator;
        this.id = UUID.randomUUID();
    }

    final ConnectionStateListener listener = new ConnectionStateListener() {

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {

            switch (newState) {

                case CONNECTED:
                    logger.info("ConnectionStateListener state : ", newState);
                    registerService();
                    break;

                case LOST:
                    logger.info("ConnectionStateListener state : ", newState);
                    // no need to do anything since its an ephemeral node 
                    break;

                case RECONNECTED:
                    logger.info("ConnectionStateListener state : ", newState);
                    registerService();
                    break;
            }
        }

    };


    public void register() {
        registerService();
        this.curator.getConnectionStateListenable().addListener(listener);
    }

    public void close() {
        this.deRegisterService();
        this.curator.getConnectionStateListenable().removeListener(listener);
    }

    private void registerService() {

        JSONObject json = new JSONObject();
        json.put("serviceType", "DYNAMIC");
        json.put("name", serviceName);
        json.put("id", id.toString());
        json.put("registrationTimeUTC", OffsetDateTime.now().toEpochSecond());
        json.put("address", resolveHostname());
        // TODO: add the fields below for service registration
        /*
        json.put("locations", serviceRegistration.locations);
        */

        String jsonString = json.toJSONString();
        byte[] data = jsonString.getBytes(Charsets.UTF8);
        try {
            logger.info("registering service: {}", jsonString);
            logger.info("service registration using path: {} ", nodePath());

            curator.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(nodePath(), data);

        } catch (Exception e) {
            logger.error("Error registering service", e);
            throw new RuntimeException(e);
        }

    }

    private void deRegisterService() {
        try {
            logger.info("Deleting the zookeeper service registration for path: {} ", nodePath());
            curator.delete()
                    .deletingChildrenIfNeeded()
                    .forPath(nodePath());
        } catch (Exception e) {
            logger.error("Error unregistering service", e);
            throw new RuntimeException(e);
        }
    }

    protected String nodePath() {
        return this.basePath + serviceName + "/" + id.toString();
    }

    protected String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Unable to determine local hostname", e);
        }
    }

}
