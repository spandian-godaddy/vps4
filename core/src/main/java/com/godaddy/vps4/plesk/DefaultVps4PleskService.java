package com.godaddy.vps4.plesk;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.godaddy.vps4.network.NetworkService;

import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class DefaultVps4PleskService implements Vps4PleskService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4PleskService.class);

    private final PleskService pleskService;
    private final NetworkService networkService;

    private final int timeoutValue;
    private Vps4Poller<PleskAction, Integer, String> pleskActionPoller;

    @Inject
    public DefaultVps4PleskService(PleskService pleskService, NetworkService networkService, Config config, Vps4Poller<PleskAction, Integer, String> pleskActionPoller) {
        this(pleskService, networkService, Integer.parseInt(config.get("vps4.callable.timeout", "5000")), pleskActionPoller);
    }

    public DefaultVps4PleskService(PleskService pleskService, NetworkService networkService, Integer timeoutValue, Vps4Poller<PleskAction, Integer, String> pleskActionPoller) {
        this.pleskService = pleskService;
        this.networkService = networkService;
        this.timeoutValue = timeoutValue;
        this.pleskActionPoller = pleskActionPoller;
    }

    private String getVmIp(long hfsVmId) {
        return networkService.getVmPrimaryAddress(hfsVmId).ipAddress;
    }

    @Override
    public List<PleskSubscription> listPleskAccounts(long hfsVmId) throws ParseException, PollerTimedOutException, Exception {

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
            logger.warn("Error parsing plesk account list response ", pe);
            throw pe;
        }
        catch (PollerTimedOutException pte) {
            logger.warn("Timed out while waiting to receive response from hfs plesk endpoint for hfsVmId: {} ", hfsVmId);
            throw pte;
        }
    }

    @SuppressWarnings("unchecked")
    private List<PleskSubscription> parseSiteListPayload(String responsePayload) throws ParseException, Exception {

        JSONParser parser = new JSONParser();
        JSONObject payloadJson = (JSONObject) parser.parse(responsePayload);
        logger.info("Response pay load: {} ", responsePayload);

        JSONArray subscriptionsArray = (JSONArray) payloadJson.get("subscriptions");
        if (subscriptionsArray != null) {
            return (List<PleskSubscription>) subscriptionsArray.stream()
                    .map(PleskSubscription::new)
                    .collect(Collectors.toList());
        }
        String message = "Could not parse the subscription list. ";
        logger.error(message);
        throw new Exception(message);
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
            logger.info("SSO URL from HFS: {} ", ssoUrl);
            // replace hostname with IP. this is a temporary fix until HFS updates their API
            String ip = getVmIp(hfsVmId);
            Pattern p = Pattern.compile("(?<=https?://).*?(?=:8443)");
            ssoUrl = p.matcher(ssoUrl).replaceFirst(ip);
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
