package com.godaddy.vps4.panopta;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Provider;

public class PanoptaClientServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    @Inject
    PanoptaClientRequestFilter panoptaClientRequestFilter;

    @Inject
    PanoptaClientResponseFilter panoptaClientResponseFilter;

    @Inject
    public PanoptaClientServiceProvider(String baseUrl, Class<T> serviceClass) {
        super(baseUrl, serviceClass);
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(panoptaClientRequestFilter);
        return requestFilters;
    }


    @Override
    public List<ClientResponseFilter> getResponseFilters() {
        List<ClientResponseFilter> responseFilters = super.getResponseFilters();
        responseFilters.add(panoptaClientResponseFilter);
        return responseFilters;
    }

}
