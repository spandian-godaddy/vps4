package com.godaddy.vps4.web.servicediscovery;


import java.time.OffsetDateTime;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Charsets;
import com.godaddy.vps4.util.ZooKeeperClient;


public class ServiceRegistrationContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistrationContextListener.class);

    private final ServiceRegistration serviceRegistration;

    private final String basePath;

    private final CuratorFramework curator;

    public ServiceRegistrationContextListener(ServiceRegistration registration, String basePath) {
        this(registration, basePath, ZooKeeperClient.getInstance());
    }

    public ServiceRegistrationContextListener(ServiceRegistration registration, String basePath, CuratorFramework curator) {
        this.serviceRegistration = registration;
        this.basePath = basePath;
        this.curator = curator;
    }

    protected String nodePath(ServiceRegistration serviceRegistration) {

        return basePath + serviceRegistration.name + "/" + serviceRegistration.id.toString();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        JSONObject json = new JSONObject();
        json.put("serviceType", "DYNAMIC");
        json.put("name", serviceRegistration.name);
        json.put("locations", serviceRegistration.locations);
        json.put("id", serviceRegistration.id.toString());
        json.put("address", serviceRegistration.address);
        json.put("registrationTimeUTC", OffsetDateTime.now().toEpochSecond());
        if (serviceRegistration.sslPort > 0) {
            json.put("sslPort", serviceRegistration.sslPort);
        }
        json.put("port", serviceRegistration.port);

        String jsonString = json.toJSONString();
        byte[] data = jsonString.getBytes(Charsets.UTF8);
        try {
            logger.info("registering service: {}", jsonString);

            curator.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(nodePath(serviceRegistration), data);

        } catch (Exception e) {
            logger.error("Error registering service", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        try {
            curator.delete()
                .deletingChildrenIfNeeded()
                .forPath(nodePath(serviceRegistration));
        } catch (Exception e) {
            logger.error("Error unregistering service", e);
            throw new RuntimeException(e);
        }
    }

}

