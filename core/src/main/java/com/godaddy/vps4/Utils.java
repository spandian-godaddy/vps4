package com.godaddy.vps4;

import java.security.Key;
import java.util.Base64;

import com.godaddy.hfs.crypto.tools.EncryptionUtil;

public class Utils {
    public static void main(String[] args) {
        /*
        Steps for decrypting a password.
        Step 1. The key used for encrypting/decrypting vps4 server passwords is itself encrypted and stored in our git repo.
                This password encryption key is encrypted using the vps4 environment specific key that is not checked into
                any git repo. The only place this key is readily available is on the jenkins build slave.
                The first step is therefore to get on the jenkins slave box and get hold of the environment specific
                master encryption key.
                a. ssh into the jenkins build slave p3plvps4cicd01.cloud.phx3.gdg
                b. cd into the /opt/vps4/keys folder.
                c. Locate the appropriate key for the environment and copy (or scp) it into the
                   ${vps4_code_path_root}/vps4/core/src/main/resources/com/godaddy/vps4/config folder.
                   For example the key for the ams3 environment is named vps4.prod_ams3.key.
        Step 2. Use the master key to decrypt the other environment keys and certs using the maven decrypt job.
                Refer to the intellij_run_configs.md document for how to create the run config to do this directly from
                intellij.
                Alternatively, to do this from the command line, cd into the core directory and run this
                maven command `mvn -Dvps4.env=prod_ams3 exec:java@decrypt-config`. This command use ams3 as an example.
                If this runs successfully then you should see the decrypted version of the `password_encryption.key`.
                This should be present in the appropriate vps4 config folder for the environment. Again, using prod_ams3
                as an example the location of the folder would be
                ${vps4_code_path_root}/vps4/core/src/main/resources/com/godaddy/vps4/config/prod_ams3.
        Step 3. Open the password_encryption.key and copy the string in there. Replace the CHANGETHISTOTHEPASSWORDENCKEY
                entry on the keyStr line with this copied value.
        Step 4. Locate the encrypted password that the customer used by examining the appropriate orchestration engine
                command execution graph. For example, the password the customer used while setting up a vm would show
                up in the CREATE_VM orchestration engine command execution graph. If the customer has performed a successful
                credentials change after provisioning a vm then the orchestration engine command to examine would be
                the one corresponding to that and not the create vm orchestration engine graph.
        Step 5. Replace CHANGETHISTOTHEENCRYPTEDPASSWORD with this value.
        Step 6. Execute the main method. If the password encryption/decryption key string is correct then the decrypted
                password will be echoed to the console.
         */
        String keyStr = "CHANGETHISTOTHEPASSWORDENCKEY";
        String password = "CHANGETHISTOTHEENCRYPTEDPASSWORD";
        Key key = EncryptionUtil.readAesKey(Base64.getDecoder().decode(keyStr.getBytes()));
        byte[] encryptedPassword = Base64.getDecoder().decode(password.getBytes());

        try {
            String decryptedPassword = new String(EncryptionUtil.decryptAes(encryptedPassword, key));
            System.out.println("Password is: " + decryptedPassword);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
