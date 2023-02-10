package com.godaddy.hfs.web;

import java.io.IOException;

import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerConnectorProviderBase {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerConnectorProviderBase.class);
    
    protected static final int MAX_PORTS_TO_TRY = 20;
    
    protected static void autoBindPort(ServerConnector connector, final int startPort) {
        autoBindPort(connector, startPort, MAX_PORTS_TO_TRY);
    }
    
    /**
    *
    * @param connector
    * @param startPort
    *            - the first port that should be attempted to be bound to
    *            (inclusive)
    * @param endPort
    *            - the number of ports to try (incrementing from startPort)
    * @return the port that was bound to
    */
   protected static void autoBindPort(ServerConnector connector, final int startPort, final int count) {

       final int endPort = startPort + count;
       int port = startPort;

       while (port < endPort) {
           try {
               logger.debug("Attempting to bind on port {}", port);
               connector.setPort(port);
               connector.open();
               break;
           } catch (IOException e) {
               port++;
           }
       }
       
       if(!connector.isOpen()) {
           throw new IllegalStateException("Unable to bind to any ports in range [" + startPort + ", " + endPort + ")");
       }

       logger.info("Bound connector to port {}", port);
       
   }

}
