package com.godaddy.vps4.client;

import com.godaddy.vps4.util.KeyManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import javax.net.ssl.KeyManager;

public class ClientCertAuthenticatedServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    private static final Logger logger = LoggerFactory.getLogger(ClientCertAuthenticatedServiceProvider.class);
    static volatile KeyManager keyManager;
    private final String clientCertificateKeyPath;
    private final String clientCertificatePath;

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

        KeyManager[] keyManagers = { keyManager };
        return keyManagers;
    }

    @Override
    public T get() {
        return super.get();
    }
}
