package com.godaddy.hfs.zookeeper;

import org.apache.curator.framework.CuratorFramework;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ZooKeeperModule extends AbstractModule {

    @Override
    public void configure() {
        bind(CuratorFramework.class)
            .toProvider(() -> ZooKeeperClient.getInstance())
            .in(Scopes.SINGLETON);
    }

}
