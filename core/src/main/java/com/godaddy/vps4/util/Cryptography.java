package com.godaddy.vps4.util;

import java.security.Key;
import java.util.Base64;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.crypto.tools.EncryptionUtil;
import com.google.inject.Inject;

public class Cryptography {
    
    private Key key;

    @Inject
    public Cryptography(Config config) {
        key = getVps4EncryptionKey(config);
    }
    
    public byte[] encrypt(String source) {
        try {
        return EncryptionUtil.encryptAes(source.getBytes(), key);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public String decrypt(byte[] encryptedData) {
        try {
            return new String(EncryptionUtil.decryptAes(encryptedData, key));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Key getVps4EncryptionKey(Config config) {
        return EncryptionUtil.readAesKey(Base64.getDecoder().decode(config.getData("PasswordEncryption.key")));
    }
}