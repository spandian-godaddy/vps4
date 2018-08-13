package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.util.List;

import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.network.IpAddress;

public interface Vps4CpanelService {

    List<CPanelAccount> listCpanelAccounts(long hfsVmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    List<String> listAddOnDomains(long hfsVmId, String username)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    CPanelSession createSession(long hfsVmId, String username, IpAddress ip, CpanelServiceType serviceType)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

}
