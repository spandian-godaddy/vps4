package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.network.NetworkService;

public class DefaultVps4CpanelService implements Vps4CpanelService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4CpanelService.class);

    final CpanelAccessHashService accessHashService;
    final NetworkService networkService;

    final int timeoutVal;
    private static final long SUCCESS = 1;

    @Inject
    public DefaultVps4CpanelService(CpanelAccessHashService accessHashService, NetworkService networkService, Config conf) {
        this(accessHashService, networkService, Integer.parseInt(conf.get("vps4.callable.timeout", "10000")));
    }

    public DefaultVps4CpanelService(CpanelAccessHashService accessHashService, NetworkService networkService, int timeoutVal) {
        this.accessHashService = accessHashService;
        this.networkService = networkService;
        this.timeoutVal = timeoutVal;
    }

    private String getVmIp(long hfsVmId) {
        return networkService.getVmPrimaryAddress(hfsVmId).ipAddress;
    }

    private String getOriginatorIp() {
        return "172.19.46.185";
    }

    interface CpanelClientHandler<T> {
        T handle(CpanelClient client)
                throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;
    }

    interface CpanelCall {
        String handle() throws CpanelAccessDeniedException, IOException;
    }

    interface SuccessHandler<T> {
        T handle(JSONObject dataJson);
    }

    interface ErrorHandler {
        Void handle(String reason);
    }

    protected CpanelClient getCpanelClient(String publicIp, String accessHash){
        return new CpanelClient(publicIp, accessHash);
    }

    <T> T withAccessHash(long hfsVmId, CpanelClientHandler<T> handler)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {

        Instant timeoutAt = Instant.now().plus(timeoutVal, ChronoUnit.MILLIS);

        Exception lastThrown = null;

        while (Instant.now().isBefore(timeoutAt)) {
            // TODO remove the hardcoded values for IP
            String fromIp = getOriginatorIp();
            String publicIp = getVmIp(hfsVmId);
            String accessHash = accessHashService.getAccessHash(hfsVmId, publicIp, fromIp, timeoutAt);
            if (accessHash == null) {
                // we couldn't get the access hash, so no point in even
                // trying to contact the VM

                // TODO throw this as CpanelAccessDeniedException?
                return null;
            }

            // TODO make sure we're still within timeoutAt to actually
            //      make the call to the VM
            CpanelClient cPanelClient = getCpanelClient(publicIp, accessHash);
            try {
                // need to configure read timeout in HTTP client
                return handler.handle(cPanelClient);

            } catch (CpanelAccessDeniedException e) {

                logger.warn("Access denied for cPanel VM {}, invalidating access hash", publicIp);

                // we weren't able to access the target VM, which may be due to an
                // access hash we thought was good, but has now been invalidated,
                // so invalidate the access hash so a new one will be attempted
                //cached.invalidate(fetchedAt);
                accessHashService.invalidAccessHash(hfsVmId, accessHash);
                lastThrown = e;

            } catch (IOException e) {
                logger.warn("Unable communicating with VM " + hfsVmId, e);
                // we timed out attempting to connect/read from the target VM
                // or we had some other transport-level issue
                lastThrown = e;
            }

        }
        // if we've run out of time communicating with the VM, but our last issue
        // was that we were having auth issues, bubble that exception back up to
        // the client, since that's the best description of the troubles we're having
        if (lastThrown != null && lastThrown instanceof CpanelAccessDeniedException) {
            throw (CpanelAccessDeniedException)lastThrown;
        }

        // any other issue is bubbled as a general timeout exception
        throw new CpanelTimeoutException("Timed out retrying an operation on VM " + hfsVmId);
    }

    @Override
    public List<CPanelAccount> listCpanelAccounts(long hfsVmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {

        return withAccessHash(hfsVmId, cPanelClient -> {

            JSONParser parser = new JSONParser();

            String sitesJson = cPanelClient.listSites();
            logger.debug("sites JSON: {}", sitesJson);
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(sitesJson);

                List<CPanelAccount> domains = new ArrayList<>();

                JSONObject data = (JSONObject) jsonObject.get("data");
                if (data != null) {
                    JSONArray accnts = (JSONArray) data.get("acct");
                    if (accnts != null) {
                        for (Object object : accnts) {
                            JSONObject accnt = (JSONObject) object;
                            String domain = (String) accnt.get("domain");
                            String username = (String) accnt.get("user");
                            if (domain != null) {
                                domains.add(new CPanelAccount(domain, username));
                            }
                        }
                    }
                }

                return domains;
            } catch (ParseException e) {
                throw new IOException("Error parsing cPanel account list response", e);
            }
        });
    }

    @Override
    public List<String> listAddOnDomains(long hfsVmId, String username)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {
        return withAccessHash(hfsVmId, cPanelClient -> {
            JSONParser parser = new JSONParser();
            String sitesJson = cPanelClient.listAddOnDomains(username);
            try {
                List<String> domains = new ArrayList<>();
                JSONObject jsonObject = (JSONObject) parser.parse(sitesJson);
                JSONObject cpanelResult = (JSONObject) jsonObject.get("cpanelresult");
                if (cpanelResult != null) {
                    JSONArray data;
                    try{
                        data = (JSONArray) cpanelResult.get("data");
                    } catch (ClassCastException e){
                        String error = (String) cpanelResult.get("error");
                        if (error != null && error.equals("User parameter is invalid or was not supplied")){
                            // cpanel will still return a 200 return status even if there's an error,
                            //  so checking the error value is the next best way to detect this error.
                            throw new CpanelInvalidUserException("User parameter ("+username+") is invalid or was not supplied");
                        }
                        throw e;
                    }

                    for (Object object : data) {
                        JSONObject addOnDomain = (JSONObject) object;
                        String domain = (String) addOnDomain.get("domain");
                        domains.add(domain);
                    }
                }
                return domains;
            } catch (ParseException | ClassCastException e) {
                throw new IOException("Error parsing cPanel account list response", e);
            }
        });
    }

    @Override
    public CPanelSession createSession(long hfsVmId, String username, IpAddress ip, CpanelServiceType serviceType)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {

        return withAccessHash(hfsVmId, cPanelClient -> cPanelClient.createSession(username, ip.ipAddress, serviceType));
    }

    private boolean didCallSucceed(JSONObject responseJson) {
        JSONObject metadata = (JSONObject) responseJson.get("metadata");
        // https://documentation.cpanel.net/display/DD/WHM+API+1+-+Return+Data
        return metadata != null && ((Long) metadata.get("result") == SUCCESS);
    }

    private String getFailureReason(JSONObject responseJson) {
        JSONObject metadata = (JSONObject) responseJson.get("metadata");
        // https://documentation.cpanel.net/display/DD/WHM+API+1+-+Return+Data
        return metadata != null ? ((String) metadata.get("reason")) : ("No reason provided");
    }

    private <T> T handleCpanelCall(String callName, CpanelCall callFn, SuccessHandler<T> successHandler,
                                   ErrorHandler errorHandler) throws CpanelAccessDeniedException, IOException {

        try {
            String response = callFn.handle();
            logger.debug("Response for call [{}]: {}", callName, response);

            JSONParser parser = new JSONParser();
            JSONObject responseJson = (JSONObject) parser.parse(response);
            if (didCallSucceed(responseJson)) {
                JSONObject data = (JSONObject) responseJson.get("data");
                if (data != null) {
                    return successHandler.handle(data);
                }
            }
            else {
                String reason = getFailureReason(responseJson);
                errorHandler.handle(reason);

            }

            throw new RuntimeException("Error while handling response for call " + callName);
        } catch (ParseException e) {
            throw new IOException("Parse error while handling response for call " + callName, e);
        }
    }

    @Override
    public Long calculatePasswordStrength(long hfsVmId, String password)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {
        return withAccessHash(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+get_password_strength
            return handleCpanelCall(
                    "calculatePasswordStrength",
                    () -> cPanelClient.calculatePasswordStrength(password),
                    dataJson -> (Long) dataJson.get("strength"),
                    reason -> {
                        throw new RuntimeException("Password strength calculation failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public Void createAccount(long hfsVmId, String domainName, String username, String password, String plan)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {
        return withAccessHash(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+createacct
            return handleCpanelCall(
                    "createAccount",
                    () -> cPanelClient.createAccount(domainName, username, password, plan),
                    dataJson -> {
                        return null;
                    },
                    reason -> {
                        throw new RuntimeException("WHM account creation failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public List<String> listPackages(long hfsVmId) throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {
        return withAccessHash(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+listpkgs
            return handleCpanelCall(
                "listPackages",
                    () -> cPanelClient.listPackages(),
                    dataJson -> {
                        JSONArray pkgsJson = (JSONArray) dataJson.get("pkg");
                        if (pkgsJson != null) {
                            List<String> packages = new ArrayList<>();
                            for (Object object : pkgsJson) {
                                JSONObject pkgJsonObj = (JSONObject) object;
                                String pkg = (String) pkgJsonObj.get("name");
                                if (pkg != null) {
                                    packages.add(pkg);
                                }
                            }

                            return packages;
                        }
                        throw new RuntimeException("No cpanel packages present");
                    },
                    reason -> {
                        throw new RuntimeException("WHM list package failed due to reason: " + reason);
                    }
                );
        });
    }
}
