package com.godaddy.vps4.web.security.crypto;

import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

/**
 * An RSA key pair is made up of:
 *
 *
 * certificate: format_name
 *
 *
 *
 * public key: e - public key exponent n - key modulus
 *
 * private key: d - private key exponent
 *
 *
 * @author bdiekelman
 *
 */

public class RSAKeyPair {

    private final RSAPublicKey publicKey;

    private final RSAPrivateCrtKey privateKey;

    public RSAKeyPair(RSAPublicKey publicKey, RSAPrivateCrtKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateCrtKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((privateKey == null) ? 0 : privateKey.hashCode());
        result = prime * result
                + ((publicKey == null) ? 0 : publicKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RSAKeyPair other = (RSAKeyPair) obj;
        if (privateKey == null) {
            if (other.privateKey != null)
                return false;
        }
        else if (!privateKey.equals(other.privateKey))
            return false;
        if (publicKey == null) {
            if (other.publicKey != null)
                return false;
        }
        else if (!publicKey.equals(other.publicKey))
            return false;
        return true;
    }

}
