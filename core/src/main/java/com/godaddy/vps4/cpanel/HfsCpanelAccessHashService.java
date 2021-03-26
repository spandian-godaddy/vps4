package com.godaddy.vps4.cpanel;

import java.time.Instant;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HfsCpanelAccessHashService implements CpanelAccessHashService {

    private static final Logger logger = LoggerFactory.getLogger(HfsCpanelAccessHashService.class);

    final CPanelService cPanelService;

    public HfsCpanelAccessHashService(CPanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public void invalidAccessHash(long vmId, String accessHash) {
        // Do nothing
        return;
    }

    @Override
    public String getAccessHash(long vmId, String publicIp, Instant timeoutAt) {
        try {
            return getAccessHashFromHFS(vmId, publicIp, timeoutAt);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String makeCallOutToCPanelVertical(long vmId, String publicIp, Instant timeoutAt) throws Exception {
        logger.info("Sending access hash generation request to HFS for vm: {}", vmId);
        CPanelAction hfsAction = this.cPanelService.requestAccess(vmId, publicIp);

        while (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)
                && !hfsAction.status.equals(CPanelAction.Status.FAILED)
                && Instant.now().isBefore(timeoutAt)) {
            logger.info("waiting on generate cpanel access hash: {}", hfsAction);

            Thread.sleep(1000);
            hfsAction = cPanelService.getAction(hfsAction.actionId);
        }

        if (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)) {
            logger.warn("failed to generate access hash {}", hfsAction);
            throw new RuntimeException("CPanel generate access hash failed");
        }

        logger.info("generate access hash complete: {}", hfsAction);
        return hfsAction.responsePayload;
    }

    private String getAccessHashFromHFS(long vmId, String publicIp, Instant timeoutAt) throws Exception {
        logger.info("sending HFS request to access cPanel VM (generate access hash) for vmId {}", vmId);

        String payload = makeCallOutToCPanelVertical(vmId, publicIp, timeoutAt);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(payload);
        String accessHash = (String) jsonObject.get("cphash");
        return accessHash;
    }

}
