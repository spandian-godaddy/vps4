package com.godaddy.vps4.tools;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.godaddy.hfs.io.Charsets;

public class EncryptionUtil {

	public static final String RSA_TRANSFORM = "RSA/ECB/PKCS1Padding";

	public static final String AES_TRANSFORM = "AES/CBC/PKCS5Padding";

	public static SecretKey generateAesKey() throws Exception {
	    KeyGenerator aesGenerator = KeyGenerator.getInstance("AES");
        aesGenerator.init(256);

        return aesGenerator.generateKey();
	}

	public static byte [] generateIV() {
		SecureRandom random = new SecureRandom();
		byte [] iv = new byte [16];
		random.nextBytes( iv );
		return iv;
	}

	public static byte[] encryptAes(byte[] text, Key key) throws Exception {
    	return encryptAes(text, key, generateIV());
    }

    public static byte[] encryptAes(byte[] text, Key key, byte[] iv) throws Exception {

        Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] encryptedBytes = cipher.doFinal(text);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Base64.getEncoder().encode(iv));
        baos.write(':');
        baos.write(Base64.getEncoder().encode(encryptedBytes));
        return baos.toByteArray();
    }

    public static byte[] decryptAes(String text, Key key) throws Exception {
    	String[] parts = text.split(":");
    	byte[] iv = Base64.getDecoder().decode(parts[0]);
    	byte[] encrypted = Base64.getDecoder().decode(parts[1]);
    	return decryptAes(encrypted, key, iv);
    }

    public static byte[] decryptAes(byte[] text, Key key) throws Exception {

        return decryptAes(new String(text, Charsets.UTF8), key);
    }

    public static byte[] decryptAes(byte[] text, Key key, byte[] iv) throws Exception {

        Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        return cipher.doFinal(text);
    }

    public static Key readAesKey(byte[] keyBytes) {

    	return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] decryptRsa(byte[] text, Key key) throws Exception {
    	Cipher cipher = Cipher.getInstance(RSA_TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        return cipher.doFinal(text);
    }

    public static byte[] encryptRsa(byte[] text, Key key) throws Exception {
    	Cipher cipher = Cipher.getInstance(RSA_TRANSFORM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(text);
    }

}
