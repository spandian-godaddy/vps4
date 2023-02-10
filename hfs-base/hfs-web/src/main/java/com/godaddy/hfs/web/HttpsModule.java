package com.godaddy.hfs.web;

import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jetty.server.ServerConnector;

import com.godaddy.hfs.web.server.HttpsServerConnectorProvider;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class HttpsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(KeyManagerFactory.class).toProvider(KeyManagerFactoryProvider.class).in(Singleton.class);
        bind(TrustManagerFactory.class).toProvider(TrustManagerFactoryProvider.class).in(Singleton.class);       
        
        Multibinder<ServerConnector> mb = Multibinder.newSetBinder(binder(), ServerConnector.class);

        mb.addBinding().toProvider(HttpsServerConnectorProvider.class).in(Singleton.class);
    }
    
}
