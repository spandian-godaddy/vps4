package com.godaddy.hfs.web;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.godaddy.hfs.config.Config;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class HttpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ServerConnectorProviderBase.class);
        
        Multibinder<ServerConnector> mb = Multibinder.newSetBinder(binder(), ServerConnector.class);

        mb.addBinding().toProvider(HttpServerConnectorProvider.class).in(Singleton.class);
    }

    static class HttpServerConnectorProvider extends ServerConnectorProviderBase implements Provider<ServerConnector> {

        static final String HTTP_PORT_START = "8000";
        
        final Config config;

        final Server server;

        @Inject
        public HttpServerConnectorProvider(Config config, Server server) {
            this.config = config;
            this.server = server;
        }

        @Override
        public ServerConnector get() {

            int httpPort = Integer.parseInt(config.get("hfs.http.port", HTTP_PORT_START));
            if (httpPort == 0) {
                return null;
            }
            
            ServerConnector httpConnector = new ServerConnector(server);
            autoBindPort(httpConnector, httpPort);


            return httpConnector;
        }

    }

}
