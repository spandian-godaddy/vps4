package com.godaddy.vps4.cdn;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Provider;

import javax.inject.Inject;
import javax.ws.rs.client.ClientResponseFilter;
import java.util.List;

public class CdnClientServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    @Inject
    CdnClientResponseFilter cdnClientResponseFilter;

    @Inject
    public CdnClientServiceProvider(String baseUrlConfigPropName, Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

    @Override
    public List<ClientResponseFilter> getResponseFilters() {
        List<ClientResponseFilter> responseFilters = super.getResponseFilters();
        responseFilters.add(cdnClientResponseFilter);
        return responseFilters;
    }
}
