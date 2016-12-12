package com.godaddy.vps4.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.crypto.PEMFile;
import com.godaddy.vps4.Environment;

/**
 *   /com/godaddy/vps4/config/{vps4.env}/config.unenc.properties
 *     => /com/godaddy/vps4/config/{vps4.env}/config.enc.properties
 *
 *  openssl genpkey -algorithm RSA -out vps4.${VPS4_ENV}.priv.pem -pkeyopt rsa_keygen_bits:4096
 *  openssl rsa -pubout -in vps4.${VPS4_ENV}.priv.pem -out vps4.${VPS4_ENV}.pub.pem
 *
 */
public class EncryptionConfig {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionConfig.class);

    enum Mode {
        DECRYPT, ENCRYPT
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage: [cmd] encrypt|decrypt");
            return;
        }

        Mode mode = null;
        if (args[0].equalsIgnoreCase("encrypt")) {
            mode = Mode.ENCRYPT;
        } else if (args[0].equalsIgnoreCase("decrypt")) {
            mode = Mode.DECRYPT;
        }
        if (mode == null) {
            System.err.println("Unknown mode: " + args[0]);
            return;
        }

        List<String> envs = Arrays.stream(Environment.values())
                .map( env -> env.getLocalName().toLowerCase() )
                .collect(Collectors.toList());

        for (String env : envs) {

            switch(mode) {
            case DECRYPT: decrypt(env); break;
            case ENCRYPT: encrypt(env); break;
            }
        }

    }

    static void encrypt(String env) throws Exception {
        logger.info("looking for encrypted properties for environment: {}", env);
        Path unencryptedFile = unencryptedPath(env);
        if (!Files.exists(unencryptedFile)) {
            return;
        }

        PublicKey publicKey = readPublicKey(env);

        Path encryptedFile = encryptedPath(env);

        // encrypt vps4.unenc.properties => vps4.enc.properties
        // (and if successful delete the unencrypted resource)
        logger.info("encrypting {} => {}", unencryptedFile, encryptedFile);
        Files.write(encryptedFile,
                encrypt(Files.readAllBytes(unencryptedFile), publicKey));
    }

    static void decrypt(String env) throws Exception {
        logger.info("looking for encrypted properties for environment: {}", env);
        Path encryptedFile = encryptedPath(env);
        if (!Files.exists(encryptedFile)) {
            return;
        }

        PrivateKey privateKey = readPrivateKey(env);

        Path unencryptedFile = unencryptedPath(env);

        // encrypt vps4.unenc.properties => vps4.enc.properties
        // (and if successful delete the unencrypted resource)
        logger.info("decrypting {} => {}", encryptedFile, unencryptedFile);
        Files.write(unencryptedFile,
                decrypt(Files.readAllBytes(encryptedFile), privateKey));
    }


    static Path encryptedPath(String env) {
        return Paths.get("src/main/resources/com/godaddy/vps4/config/" + env + "/config.enc.properties");
    }

    static Path unencryptedPath(String env) {
        return Paths.get("src/main/resources/com/godaddy/vps4/config/" + env + "/config.unenc.properties");
    }

    static PrivateKey readPrivateKey(String env) throws Exception {
     // get the private key for this environment
        String privateKeyPath = "/vps4." + env + ".priv.pem";
        InputStream is = EncryptionConfig.class.getResourceAsStream(privateKeyPath);
        if (is == null) {
            throw new Exception("Private key for environment " + env + " not found at: " + privateKeyPath);
        }
        PrivateKey privateKey = PEMFile.readPEM(new BufferedReader(new InputStreamReader(is))).getPrivateKey();
        if (privateKey == null) {
            throw new Exception("No private key found in resource: " + privateKeyPath);
        }
        return privateKey;
    }

    static PublicKey readPublicKey(String env) throws Exception {
        // get the private key for this environment
        String privateKeyPath = "/vps4." + env + ".pub.pem";
        InputStream is = EncryptionConfig.class.getResourceAsStream(privateKeyPath);
        if (is == null) {
            throw new Exception("Public key for environment " + env + " not found at: " + privateKeyPath);
        }
        PublicKey publicKey = PEMFile.readPEM(new BufferedReader(new InputStreamReader(is))).getPublicKey();
        if (publicKey == null) {
            throw new Exception("No public key found in resource: " + privateKeyPath);
        }
        return publicKey;
    }

    static final String CIPHER = "RSA/ECB/PKCS1Padding";

    public static byte[] encrypt(byte[] text, PublicKey key) throws Exception {

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(text);
    }

    public static byte[] decrypt(byte[] text, PrivateKey key) throws Exception {

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key);

        return cipher.doFinal(text);
    }

}
