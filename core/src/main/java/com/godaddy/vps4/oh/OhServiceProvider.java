package com.godaddy.vps4.oh;

import java.util.List;

import javax.ws.rs.client.ClientRequestFilter;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class OhServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    @Inject Config config;
    private final String zone;

    public OhServiceProvider(String zone, Class<T> serviceClass) {
        super("oh." + zone + ".url", serviceClass);
        this.zone = zone;
    }

    private OhServiceRequestFilter getAuthFilter() {
        String auth = config.get("oh." + zone + ".auth");
        return new OhServiceRequestFilter(auth);
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(getAuthFilter());
        return requestFilters;
    }
}
