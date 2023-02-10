package com.godaddy.hfs.crypto.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * .enc file format:
 * iv:base64 ':' ciphertext:base64
 *
 * iv - random seed for each block cipher (generated on a per-file basis)
 *
 */
public class EncryptionConfig {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionConfig.class);

    enum Mode {
        DECRYPT, ENCRYPT
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: [cmd] [encrypt key]|[decrypt key] configpath");
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

        Path configPath = Paths.get(args[1]);
        if (!Files.exists(configPath)) {
        	System.err.println("Config path not found: " + configPath);
        	return;
        }

        Path keyPath = Paths.get(args[2]);
        if (!Files.exists(keyPath)) {
        	System.err.println("Key not found: " + keyPath);
        	return;
        }

        Key key = EncryptionUtil.readAesKey(
                Base64.getDecoder().decode(
                        Files.readAllBytes(keyPath)));

        switch(mode) {
        case DECRYPT:

        	decrypt(configPath, key);
        	break;

        case ENCRYPT:

        	encrypt(configPath, key);
        	break;
        }

    }

    static void encrypt(Path path, Key key) throws Exception {

    	if (Files.isDirectory(path)) {
    		// recursively decrypt files in child directories
    		Files.list(path).forEach(child -> {
    			try {
    				encrypt(child, key);
    			} catch (Exception e) {
    				throw new RuntimeException(e);
    			}
        	});

		} else if (Files.isRegularFile(path)) {

			String filename = path.getFileName().toString();

			if (filename.endsWith(".unenc")) {

				// ".unenc => .enc"
				String encryptedFilename = filename.substring(
						0,
						filename.length() - ".unenc".length()) + ".enc";
				Path encryptedPath = path.resolveSibling(encryptedFilename);

				logger.info("encrypting: {}", filename);

				Files.write(encryptedPath,
		                EncryptionUtil.encryptAes(Files.readAllBytes(path), key));
			}
		}
    }



    static void decrypt(Path path, Key key) throws Exception {

    	if (Files.isDirectory(path)) {
    		// recursively decrypt files in child directories
    		Files.list(path).forEach(child -> {
    			try {
    				decrypt(child, key);
    			} catch (Exception e) {
    				throw new RuntimeException(e);
    			}
        	});

		} else if (Files.isRegularFile(path)) {

			String filename = path.getFileName().toString();

			if (filename.endsWith(".enc")) {

				// ".enc => .unenc"
				String decryptedFilename = filename.substring(
						0,
						filename.length() - ".enc".length()) + ".unenc";
				Path decryptedPath = path.resolveSibling(decryptedFilename);

				logger.info("decrypting: {}", filename);

				Files.write(decryptedPath,
		                EncryptionUtil.decryptAes(Files.readAllBytes(path), key));
			}
		}
    }



}
