package com.godaddy.vps4.jsd;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Provider;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import java.util.List;

public class JsdClientServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    @Inject
    JsdClientRequestFilter jsdClientRequestFilter;

    @Inject
    JsdClientResponseFilter jsdClientResponseFilter;

    @Inject
    public JsdClientServiceProvider(String baseUrl, Class<T> serviceClass) {
        super(baseUrl, serviceClass);
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(jsdClientRequestFilter);
        return requestFilters;
    }


    @Override
    public List<ClientResponseFilter> getResponseFilters() {
        List<ClientResponseFilter> responseFilters = super.getResponseFilters();
        responseFilters.add(jsdClientResponseFilter);
        return responseFilters;
    }

}
