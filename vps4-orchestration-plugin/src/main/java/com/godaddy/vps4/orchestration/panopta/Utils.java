package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;

public class Utils {
    public static String getServerTemplateId(Config config, VirtualMachineCredit credit) {
        String templateType = (credit.isManaged()) ? "managed" : credit.hasMonitoring() ? "addon" : "base";
        String templateOS = credit.getOperatingSystem().toLowerCase();

        return config.get("panopta.api.templates." + templateType + "." + templateOS);
    }

    public static String[] getTemplateIds(Config config, VirtualMachineCredit credit) {
        String serverTemplate = getServerTemplateId(config, credit);
        String dcAlertTemplate = config.get("panopta.api.templates.webhook");
        return new String[] { serverTemplate, dcAlertTemplate };
    }

    public static String getNonManagedTemplateId(Config config, VirtualMachineCredit credit) {
        String templateType = credit.hasMonitoring() ? "addon" : "base";
        String templateOS = credit.getOperatingSystem().toLowerCase();

        return config.get("panopta.api.templates." + templateType + "." + templateOS);
    }
}
