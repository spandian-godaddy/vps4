package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.util.List;

import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;

public interface Vps4CpanelService {

    List<CPanelAccount> listCpanelAccounts(long hfsVmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

    CPanelSession createSession(long hfsVmId, String username, CpanelServiceType serviceType)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;

}
