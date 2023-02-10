package com.godaddy.hfs.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class ServerModule extends AbstractModule {

    @Override
    public void configure() {

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(500);

        bind(Server.class).toInstance(new Server(threadPool));
        
    }
}
