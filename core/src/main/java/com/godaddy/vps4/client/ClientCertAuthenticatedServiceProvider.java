package com.godaddy.vps4.client;

import static com.godaddy.vps4.client.ClientUtils.getShopperIdInjectionFilter;

import java.util.List;

import javax.net.ssl.KeyManager;
import javax.ws.rs.client.ClientRequestFilter;

import com.godaddy.vps4.util.KeyManagerBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ClientCertAuthenticatedServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    private final String clientCertificateKeyPath;
    private final String clientCertificatePath;
    @Inject @ShopperId Provider<String> shopperIdProvider;

    private volatile KeyManager keyManager;

    public ClientCertAuthenticatedServiceProvider(String baseUrlConfigPropName,
                                                  Class<T> serviceClass,
                                                  String clientCertificateKeyPath,
                                                  String clientCertificatePath
                                              ) {
        super(baseUrlConfigPropName, serviceClass);
        this.clientCertificateKeyPath = clientCertificateKeyPath;
        this.clientCertificatePath = clientCertificatePath;
    }

    @Override
    KeyManager[] getKeyManagers() {
        if (keyManager == null) {
            keyManager = KeyManagerBuilder.newKeyManager(config, clientCertificateKeyPath, clientCertificatePath);
        }

        return new KeyManager[]{ keyManager };
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(getShopperIdInjectionFilter(shopperIdProvider));
        return requestFilters;
    }

    @Override
    public T get() {
        return super.get();
    }
}
