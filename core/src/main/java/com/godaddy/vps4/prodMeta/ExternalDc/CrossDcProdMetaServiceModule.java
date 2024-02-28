package com.godaddy.vps4.prodMeta.ExternalDc;

import static com.godaddy.vps4.client.ClientUtils.getCertJwtAuthServiceProvider;

import com.godaddy.vps4.sso.CertJwtApi;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class CrossDcProdMetaServiceModule extends AbstractModule {
    @Override
    public void configure() {
        bind(CrossDcProdMetaClientService.class)
                .toProvider(getCertJwtAuthServiceProvider(CrossDcProdMetaClientService.class,
                            "vps4.crossdc.api.base.url", 
                            CertJwtApi.VPS4))
                .in(Singleton.class);
    }
    
}
