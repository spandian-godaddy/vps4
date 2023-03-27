package com.godaddy.vps4.cpanel;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class HfsCpanelApiTokenService implements CpanelApiTokenService {

    private static final Logger logger = LoggerFactory.getLogger(HfsCpanelApiTokenService.class);

    final CPanelService cPanelService;

    public HfsCpanelApiTokenService(CPanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public void invalidateApiToken(long vmId, String accessHash) {
        return;
    }

    @Override
    public String getApiToken(long vmId, Instant timeoutAt) {
        try {
            return getApiTokenFromHFS(vmId, timeoutAt);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String makeCallOutToCPanelVertical(long vmId, Instant timeoutAt) throws Exception {
        logger.info("Sending API token generation request to HFS for vm: {}", vmId);
        CPanelAction hfsAction = this.cPanelService.requestApiToken(vmId);

        while (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)
                && !hfsAction.status.equals(CPanelAction.Status.FAILED)
                && Instant.now().isBefore(timeoutAt)) {
            logger.info("waiting on generate cpanel API token: {}", hfsAction);

            Thread.sleep(1000);
            hfsAction = cPanelService.getAction(hfsAction.actionId);
        }

        if (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)) {
            logger.warn("failed to generate API token {}", hfsAction);
            throw new RuntimeException("CPanel generate API token failed");
        }

        logger.info("generate API token complete: {}", hfsAction);
        return hfsAction.responsePayload;
    }

    private String getApiTokenFromHFS(long vmId, Instant timeoutAt) throws Exception {
        logger.info("sending HFS request to access cPanel VM (generate API token) for vmId {}", vmId);

        String payload = makeCallOutToCPanelVertical(vmId, timeoutAt);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(payload);
        String apiToken = (String) jsonObject.get("api_token");
        return apiToken;
    }
}
