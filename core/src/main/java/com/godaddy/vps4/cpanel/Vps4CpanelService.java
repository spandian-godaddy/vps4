package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.util.List;

import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;

public interface Vps4CpanelService {

    List<CPanelAccount> listCpanelAccounts(long hfsVmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    List<String> listAddOnDomains(long hfsVmId, String username)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    CPanelSession createSession(long hfsVmId, String username, CpanelServiceType serviceType)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    Long calculatePasswordStrength(long hfsVmId, String password)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    Void createAccount(long hfsVmId, String domainName, String username, String password, String plan, String contactEmail)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    List<String> listPackages(long hfsVmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

}
