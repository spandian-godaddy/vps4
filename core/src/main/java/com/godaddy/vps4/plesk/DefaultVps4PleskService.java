package com.godaddy.vps4.plesk;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.util.PollerTimedOutException;
import com.godaddy.vps4.util.Vps4Poller;

import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class DefaultVps4PleskService implements Vps4PleskService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4PleskService.class);

    private final PleskService pleskService;
    private final int timeoutValue;
    private Vps4Poller<PleskAction, Integer, String> pleskActionPoller;

    @Inject
    public DefaultVps4PleskService(PleskService pleskService, Config config, Vps4Poller<PleskAction, Integer, String> pleskActionPoller) {
        this(pleskService, Integer.parseInt(config.get("vps4.callable.timeout", "5000")), pleskActionPoller);
    }

    public DefaultVps4PleskService(PleskService pleskService, Integer timeoutValue, Vps4Poller<PleskAction, Integer, String> pleskActionPoller) {
        this.pleskService = pleskService;
        this.timeoutValue = timeoutValue;
        this.pleskActionPoller = pleskActionPoller;
    }

    @Override
    public List<PleskAccount> listPleskAccounts(long hfsVmId) throws ParseException, PollerTimedOutException, Exception {

        PleskAction pleskAction;

        try {
            logger.info("Attempting to get sites for plesk from hfs plesk vertical for hfsVmId: {} ", hfsVmId);

            // invoke hfs vertical to get site list
            pleskAction = pleskService.requestSiteList(hfsVmId);
            logger.info("plesk action returned by site list endpoint: {} ", pleskAction);
        }
        catch (Exception ex) {
            logger.error("Error: Encountered exception while requesting site list for hfsVmId: {} , exception: {} ", hfsVmId, ex);
            throw ex;
        }

        try {
            // poll the action id returned by the hfs vertical's "request access" endpoint
            String responsePayload = pleskActionPoller.poll(pleskAction, timeoutValue);
            logger.info("plesk sites in response {} ", responsePayload);

            // parse the response payload and convert to JSON
            return parseSiteListPayload(responsePayload);
        }
        catch (ParseException pe) {
            logger.warn("Error parsing plesk account list response {} ", pe);
            throw pe;
        }
        catch (PollerTimedOutException pte) {
            logger.warn("Timed out while waiting to receive response from hfs plesk endopoint for hfsVmId: {} ", hfsVmId);
            throw pte;
        }
    }

    private List<PleskAccount> parseSiteListPayload(String responsePayload) throws ParseException {
        List<PleskAccount> accounts = new ArrayList<>();

        JSONParser parser = new JSONParser();
        JSONObject payloadJson = (JSONObject) parser.parse(responsePayload);
        logger.info("Response pay load: {} ", responsePayload);

        JSONArray sitesArray = (JSONArray) payloadJson.get("sites");
        if (sitesArray != null) {

            for (Object object : sitesArray) {

                JSONObject site = (JSONObject) object;
                if (site != null) {
                    String domain = (String) site.get("name");
                    String webspace = (String) site.get("webspace");
                    String ipAddress = (String) site.get("ip_address");
                    String ftpLogin = (String) site.get("ftp_login");
                    String diskUsed = (String) site.get("diskused");

                    PleskAccount pleskAccount = new PleskAccount(domain, webspace, ipAddress, ftpLogin, diskUsed);
                    logger.info(pleskAccount.toString());
                    accounts.add(pleskAccount);
                }
            }
        }
        return accounts;
    }

    @Override
    public PleskSession getPleskSsoUrl(long hfsVmId, String fromIpAddress) throws PleskUrlUnavailableException, ParseException, PollerTimedOutException {

        logger.info("Requesting plesk access for hfsVmId: {} and from ip address: {} ", hfsVmId, fromIpAddress);

        String ssoUrl;
        PleskAction pleskAction;

        try {
            // invoke hfs vertical to request access token
            pleskAction = pleskService.requestAccess(hfsVmId, fromIpAddress);
            logger.info("plesk action returned by request access endpoint: {} ", pleskAction.toString());
        }
        catch (Exception ex) {
            logger.warn("Exception encountered when attempting to request access to Plesk VM for hfsVmId:  {} , Exception: {} ", hfsVmId, ex);
            throw ex;
        }

        try {
            // poll the action id returned by the hfs vertical's "request access" endpoint
            String responsePayload = pleskActionPoller.poll(pleskAction, timeoutValue);
            logger.info("Plesk session url response {} ", responsePayload);

            // parse the response payload and convert to json
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(responsePayload);

            ssoUrl = (String) jsonObject.get("ssoUrl");
            if (StringUtils.isEmpty(ssoUrl)) {
                String message = String.format("Error: Could not get plesk SSO Url for HFS vmId: {} ", hfsVmId);
                throw new PleskUrlUnavailableException(message);
            }
            logger.info("SSO URL: {} ", ssoUrl);
            return new PleskSession(ssoUrl);
        }
        catch (ParseException pe) {
            logger.warn("Error during parsing of response url for action {} , error: {}", pleskAction.actionId, pe);
            throw pe;
        }
        catch (PollerTimedOutException pte) {
            logger.warn("Timed out while waiting to receive response from hfs plesk endopoint for hfsVmId: {} ", hfsVmId);
            throw pte;
        }
    }
}
