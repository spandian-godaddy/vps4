package com.godaddy.vps4.web.cpanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class GetCpanelAccounts implements Callable<List<CPanelAccount>> {

    private static final Logger logger = LoggerFactory.getLogger(GetCpanelAccounts.class);

    final Long vmId;

    final String publicIp;

    final String accessHash;

    public GetCpanelAccounts(Long vmId, String publicIp, String accessHash) {
        this.vmId = vmId;
        this.publicIp = publicIp;
        this.accessHash = accessHash;
    }

    @Override
    public List<CPanelAccount> call() throws Exception {
        CpanelClient cPanelClient = new CpanelClient(publicIp, accessHash);
        JSONParser parser = new JSONParser();

        try {
            String sitesJson = cPanelClient.listSites();
            logger.debug("sites JSON: {}", sitesJson);
            JSONObject jsonObject = (JSONObject) parser.parse(sitesJson);
            JSONObject data = (JSONObject) jsonObject.get("data");
            JSONArray accnts = (JSONArray) data.get("acct");

            List<CPanelAccount> domains = new ArrayList<>();
            for (Object object : accnts) {
                JSONObject accnt = (JSONObject) object;
                domains.add(new CPanelAccount((String) accnt.get("domain")));
            }

            return domains;
        } catch (Exception e) {
            // TODO should we squelch any particular type of exception here or take some sort of remediation steps
            logger.info("Exception was thrown", e);
            throw e;
        }
    }
}
