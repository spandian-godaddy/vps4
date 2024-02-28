package com.godaddy.vps4.prodMeta.ExternalDc;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CrossDcProdMetaServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    @Inject
    public CrossDcProdMetaServiceProvider(String baseUrlConfigPropName, Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

}
