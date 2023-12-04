package com.godaddy.vps4.firewall;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.godaddy.vps4.jsd.JsdClientResponseFilter;
import com.google.inject.Provider;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import java.util.List;

public class FirewallClientServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    @Inject
    FirewallClientResponseFilter firewallClientResponseFilter;

    @Inject
    public FirewallClientServiceProvider(String baseUrlConfigPropName, Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

    @Override
    public List<ClientResponseFilter> getResponseFilters() {
        List<ClientResponseFilter> responseFilters = super.getResponseFilters();
        responseFilters.add(firewallClientResponseFilter);
        return responseFilters;
    }
}
