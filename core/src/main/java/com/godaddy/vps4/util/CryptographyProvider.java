package com.godaddy.vps4.util;

import com.godaddy.hfs.config.Config;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CryptographyProvider implements Provider<Cryptography> {

    private final Cryptography cryptography;

    @Inject
    public CryptographyProvider(Config config) {
        cryptography = new Cryptography(config);
    }

    @Override
    public Cryptography get() {
        return cryptography;
    }
}
