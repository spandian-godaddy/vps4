package com.godaddy.vps4.ipblacklist;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;
import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Provider;

public class IpBlacklistServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    @Inject
    IpBlacklistRequestFilter ipBlacklistRequestFilter;

    @Inject
    public IpBlacklistServiceProvider(String baseUrlConfigPropName, Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(ipBlacklistRequestFilter);
        return requestFilters;
    }
}