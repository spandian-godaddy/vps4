package com.godaddy.vps4.web.security.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyPairUtil {

    private static final Logger logger = LoggerFactory.getLogger(KeyPairUtil.class);

    private static final int KEY_SIZE = 1024; // FIXME for production work, bump to 4096

    public static RSAKeyPair generate() {

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);

            logger.debug("generating {}-bit key", KEY_SIZE);

            long begin = System.currentTimeMillis();

            KeyPair keyPair = generator.genKeyPair();

            long end = System.currentTimeMillis();

            logger.debug("generated {}-bit key in {} ms", KEY_SIZE, (end - begin));

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();

            return new RSAKeyPair(publicKey, privateKey);

        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing RSA algoritm", e);
        }
    }

    public static JSONObject toJson(RSAKeyPair keyPair) {

        JSONObject json = new JSONObject();

        setInteger(json, "publicExponent", keyPair.getPublicKey().getPublicExponent());
        setInteger(json, "modulus", keyPair.getPublicKey().getModulus());

        setInteger(json, "privateExponent", keyPair.getPrivateKey().getPrivateExponent());
        setInteger(json, "primeP", keyPair.getPrivateKey().getPrimeP());
        setInteger(json, "primeQ", keyPair.getPrivateKey().getPrimeQ());
        setInteger(json, "primeExponentP", keyPair.getPrivateKey().getPrimeExponentP());
        setInteger(json, "primeExponentQ", keyPair.getPrivateKey().getPrimeExponentQ());
        setInteger(json, "crtCoefficient", keyPair.getPrivateKey().getCrtCoefficient());

        return json;
    }

    protected static void setInteger(JSONObject json, String key, BigInteger num) {
        json.put(key, Base64.getEncoder().encodeToString(num.toByteArray()));
    }

    protected static BigInteger getInteger(JSONObject json, String key) {
        return new BigInteger(Base64.getDecoder().decode((String) json.get(key)));
    }

    public static RSAKeyPair fromJson(Reader r) throws IOException, ParseException, InvalidKeySpecException {
        JSONParser parser = new JSONParser();

        JSONObject keyObj = (JSONObject) parser.parse(r);

        BigInteger publicExponent = getInteger(keyObj, "publicExponent");
        BigInteger modulus = getInteger(keyObj, "modulus");

        BigInteger privateExponent = getInteger(keyObj, "privateExponent");
        BigInteger primeP = getInteger(keyObj, "primeP");
        BigInteger primeQ = getInteger(keyObj, "primeQ");
        BigInteger primeExponentP = getInteger(keyObj, "primeExponentP");
        BigInteger primeExponentQ = getInteger(keyObj, "primeExponentQ");
        BigInteger crtCoefficient = getInteger(keyObj, "crtCoefficient");

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
        RSAPrivateKeySpec privateKeySpec = new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent,
                primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(privateKeySpec);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            return new RSAKeyPair(publicKey, privateKey);

        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not found");
        }
    }

    private static final byte[] SSH_RSA_PREFIX = new byte[] { 0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a' };

    public static String base64EncodePublicKey(RSAPublicKey publicKey) throws IOException {

        byte[] keyBytes = encodePublicKey(publicKey);

        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public static byte[] encodePublicKey(RSAPublicKey key) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        /* encode the "ssh-rsa" string */
        out.write(SSH_RSA_PREFIX);

        /* Encode the public exponent */
        BigInteger e = key.getPublicExponent();
        byte[] data = e.toByteArray();
        encodeUInt32(data.length, out);
        out.write(data);

        /* Encode the modulus */
        BigInteger m = key.getModulus();
        data = m.toByteArray();
        encodeUInt32(data.length, out);
        out.write(data);

        return out.toByteArray();
    }

    public static void encodeUInt32(int value, OutputStream out) throws IOException {
        byte[] tmp = new byte[4];
        tmp[0] = (byte) ((value >>> 24) & 0xff);
        tmp[1] = (byte) ((value >>> 16) & 0xff);
        tmp[2] = (byte) ((value >>> 8) & 0xff);
        tmp[3] = (byte) (value & 0xff);
        out.write(tmp);
    }

}
