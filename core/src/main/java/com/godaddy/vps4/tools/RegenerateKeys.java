package com.godaddy.vps4.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import javax.crypto.SecretKey;

import com.godaddy.hfs.crypto.tools.EncryptionUtil;

public class RegenerateKeys {

	public static void main(String[] args) throws Exception {

	    if (args.length < 2) {
	        System.out.println("usage: [cmd] prefix path");
	        return;
	    }

		String prefix = args[0];              // vps4
		Path targetDir = Paths.get(args[1]);  // "src/main/resources/")

		regenerate(prefix, targetDir);
	}

	public static void regenerate(String prefix, Path targetDir) throws Exception {

		String[] environments = { "local", "dev", "test", "stage", "prod", "prod_phx3", "prod_sin2", "prod_ams3" };

		for (String env : environments) {

			System.out.println("generating keys for environment: " + env);

			// generate RSA key pair
			SecretKey aesKey = EncryptionUtil.generateAesKey();

			Files.write(targetDir.resolve(prefix+"."+env+".key"),
					Base64.getEncoder().encode(aesKey.getEncoded()));
		}
	}
}