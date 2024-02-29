package com.godaddy.vps4.orchestration.hfs.plesk;

import javax.inject.Inject;

import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskImageConfigRequest;
import com.godaddy.hfs.plesk.PleskService;
import com.godaddy.vps4.vm.PleskLicenseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Cryptography;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ConfigurePlesk implements Command<ConfigurePlesk.ConfigurePleskRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurePlesk.class);

    final PleskService pleskService;

    private final Cryptography cryptography;

    @Inject
    public ConfigurePlesk(PleskService pleskService, Cryptography cryptography) {
        this.pleskService = pleskService;
        this.cryptography = cryptography;
    }

    @Override
    public Void execute(CommandContext context, ConfigurePleskRequest request) {
        logger.info("sending HFS request to config Plesk image for vmId {}", request.vmId);

        String password = cryptography.decrypt(request.encryptedPassword);

        PleskAction hfsAction = context.execute("RequestFromHFS", ctx -> pleskService.imageConfig(new PleskImageConfigRequest(request.vmId,
                                            request.username,
                                            password,
                                            getHfsPleskLicenseTypeString(request.licenseType))), PleskAction.class);

        context.execute(WaitForPleskAction.class, hfsAction);

        logger.info("Completed configuring Plesk vm action {} ", hfsAction);
        return null;
    }

    private String getHfsPleskLicenseTypeString(PleskLicenseType licenseType) {
        String hfsLicenseType = "web_host";
        if(licenseType == PleskLicenseType.PLESKWEBPRO) {
            hfsLicenseType = "web_pro";
        }
        return hfsLicenseType;
    }

    public static class ConfigurePleskRequest {
        public long vmId;
        public String username;
        public byte[] encryptedPassword;
        public PleskLicenseType licenseType;

        // Empty constructor required for Jackson
        public ConfigurePleskRequest() {}

        public ConfigurePleskRequest(long vmId, String username, byte[] encryptedPassword, PleskLicenseType pleskLicenseType) {
            this.vmId = vmId;
            this.username = username;
            this.encryptedPassword = encryptedPassword;
            this.licenseType = pleskLicenseType;
        }
    }

}
