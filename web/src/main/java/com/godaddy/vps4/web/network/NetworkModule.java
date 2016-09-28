package com.godaddy.vps4.web.network;

import javax.inject.Singleton;

import com.godaddy.vps4.hfs.HfsClientProvider;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.network.NetworkService;

public class NetworkModule extends AbstractModule {

	@Override
	protected void configure() {
        bind(NetworkService.class).toProvider(new HfsClientProvider<NetworkService>(NetworkService.class)).in(Singleton.class);
	}
}
