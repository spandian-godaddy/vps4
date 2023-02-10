package com.godaddy.hfs.servicediscovery;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.eclipse.jetty.server.ServerConnector;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.io.Charsets;
import com.google.common.base.Optional;


public class ZkServiceRegistrationContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ZkServiceRegistrationContextListener.class);

    private final Config config;

    private final CuratorFramework curator;

    ServiceRegistration serviceRegistration;

    private Set<ServerConnector> connectors;

    private HfsServiceMetadata metadata;

    @Inject
    public ZkServiceRegistrationContextListener(
            Config config,
            Optional<HfsServiceMetadata> metadata,
            CuratorFramework curator,
            Set<ServerConnector> connectors) {
        this.config = config;
        this.metadata = metadata.or(getDefaultMetadata());
        this.curator = curator;
        this.connectors = connectors;
    }

    public HfsServiceMetadata getDefaultMetadata() {        
        String serviceName = config.get("servicediscovery.zk.serviceName", "api");
        String[] locations = config.get("servicediscovery.zk.locations", "/api/").split(";");
  
        return new HfsServiceMetadata(serviceName, HfsServiceMetadata.ServiceType.OTHER, locations);
    }
    
    protected String nodePath(ServiceRegistration serviceRegistration) {

        return getZookeeperPath() + serviceRegistration.name + "/" + serviceRegistration.id.toString();
    }
    
    private String getZookeeperPath() {
        String zookeeperPath = null;
        
        switch(metadata.getServiceType()) {
            case OTHER:
                try {
                    zookeeperPath = config.get("servicediscovery.zk.path");
                } catch(IllegalStateException e) {
                    logger.error("'servicediscovery.zk.path' property not set. Service type 'OTHER' requires this property.");
                    throw new RuntimeException("'servicediscovery.zk.path' property not set. Service type 'OTHER' requires this property.", e);
                }
                break;
            case WEB:
                zookeeperPath = "/service/verticals/";
                break;
            case DAEMON:
            default:
                zookeeperPath = "/service/registrations/";
                break;
        }
        return zookeeperPath;
    }

    protected ServiceRegistration newServiceRegistration() {
        ServiceRegistration serviceRegistration = new ServiceRegistration();
            
        serviceRegistration.locations.addAll(metadata.getLocations());
        serviceRegistration.name = metadata.getServiceName();
        serviceRegistration.address = resolveHostname();

        int httpPort = 0;
        int httpsPort = 0;
        for (ServerConnector connector : connectors) {
            logger.debug("Connector protocols: {}", String.join(",", connector.getProtocols()));
            
            List<String> protocols = connector.getProtocols();
            
            if (anyStartsWith(protocols, "ssl")) {  // must come before http
                httpsPort = connector.getPort();
                logger.debug("HTTPS port is {}", httpsPort);
            } else if (anyStartsWith(protocols, "http")) {
                httpPort = connector.getPort();
                logger.debug("HTTP port is {}", httpPort);
            }
        }
       
        serviceRegistration.port = httpPort;
        serviceRegistration.sslPort = httpsPort;
        
        return serviceRegistration;
    }
    
    private boolean anyStartsWith(Collection<? extends String> iterable, String searchPrefix) {
        return iterable.stream().anyMatch(element -> element.startsWith(searchPrefix));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        this.serviceRegistration = newServiceRegistration();

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
                .forPath(nodePath(this.serviceRegistration));
        } catch (Exception e) {
            logger.error("Error unregistering service", e);
            throw new RuntimeException(e);
        }
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

