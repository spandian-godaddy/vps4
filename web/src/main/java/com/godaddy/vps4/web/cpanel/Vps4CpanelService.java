package com.godaddy.vps4.web.cpanel;

import java.util.List;

import com.godaddy.vps4.web.cpanel.CpanelClient.CpanelServiceType;

public interface Vps4CpanelService {

    List<CPanelAccount> listCpanelAccounts(long vmId);

    CPanelSession createSession(long vmId, String username, CpanelServiceType serviceType);

}
