package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.vm.HostnameGenerator;

public class DefaultVps4CpanelService implements Vps4CpanelService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4CpanelService.class);

    final CpanelApiTokenService apiTokenService;

    final NetworkService networkService;

    final int timeoutVal;
    private static final long SUCCESS = 1;

    @Inject
    public DefaultVps4CpanelService(CpanelApiTokenService apiTokenService,
                                    NetworkService networkService, Config conf) {
        this(apiTokenService,
                networkService, Integer.parseInt(conf.get("vps4.callable.timeout", "10000")));
    }

    public DefaultVps4CpanelService(CpanelApiTokenService apiTokenService,
                                    NetworkService networkService,
                                    int timeoutVal) {
        this.networkService = networkService;
        this.apiTokenService = apiTokenService;
        this.timeoutVal = timeoutVal;
    }

    private String getVmHostname(long hfsVmId) {
        IpAddress ip = networkService.getVmPrimaryAddress(hfsVmId);
        return HostnameGenerator.getLinuxHostname(ip.ipAddress);
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

    protected CpanelClient getCpanelClient(String hostname, String apiToken) {
        return new CpanelClient(hostname, apiToken);
    }

    <T> T withAccessToken(long hfsVmId, CpanelClientHandler<T> handler)
            throws CpanelAccessDeniedException, CpanelTimeoutException {

        Instant timeoutAt = Instant.now().plus(timeoutVal, ChronoUnit.MILLIS);

        Exception lastThrown = null;

        while (Instant.now().isBefore(timeoutAt)) {
            String hostname = getVmHostname(hfsVmId);
            String accessToken = apiTokenService.getApiToken(hfsVmId, timeoutAt);
            if (accessToken == null) {
                // we couldn't get the access token, so no point in even
                // trying to contact the VM

                // TODO throw this as CpanelAccessDeniedException?
                return null;
            }

            // TODO make sure we're still within timeoutAt to actually
            //      make the call to the VM
            CpanelClient cPanelClient = getCpanelClient(hostname, accessToken);
            try {
                // need to configure read timeout in HTTP client
                return handler.handle(cPanelClient);

            } catch (CpanelAccessDeniedException e) {

                logger.info("Access denied for cPanel VM {}, invalidating access token", hostname);

                // we weren't able to access the target VM, which may be due to an
                // access token we thought was good, but has now been invalidated,
                // so invalidate the access token so a new one will be attempted
                //cached.invalidate(fetchedAt);
                apiTokenService.invalidateApiToken(hfsVmId, accessToken);
                lastThrown = e;

            } catch (IOException e) {
                logger.info("Unable to communicate with VM " + hfsVmId, e);
                // we timed out attempting to connect/read from the target VM
                // or we had some other transport-level issue
                lastThrown = e;

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    logger.info("Interrupted while sleeping", ie);
                }
            }
        }
        // if we've run out of time communicating with the VM, but our last issue
        // was that we were having auth issues, bubble that exception back up to
        // the client, since that's the best description of the troubles we're having
        if (lastThrown != null && lastThrown instanceof CpanelAccessDeniedException) {
            throw (CpanelAccessDeniedException) lastThrown;
        }

        // any other issue is bubbled as a general timeout exception
        throw new CpanelTimeoutException("Timed out retrying an operation on VM " + hfsVmId);
    }

    @Override
    public List<CPanelAccount> listCpanelAccounts(long hfsVmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException {

        return withAccessToken(hfsVmId, cPanelClient -> {

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

                domains.sort(Comparator.comparing(d -> d.name));
                return domains;
            } catch (ParseException e) {
                throw new IOException("Error parsing cPanel account list response", e);
            }
        });
    }

    @Override
    public List<String> listAddOnDomains(long hfsVmId, String username)
            throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            JSONParser parser = new JSONParser();
            String sitesJson = cPanelClient.listAddOnDomains(username);
            try {
                List<String> domains = new ArrayList<>();
                JSONObject jsonObject = (JSONObject) parser.parse(sitesJson);
                JSONObject cpanelResult = (JSONObject) jsonObject.get("cpanelresult");
                if (cpanelResult != null) {
                    JSONArray data;
                    try {
                        data = (JSONArray) cpanelResult.get("data");
                    } catch (ClassCastException e) {
                        String error = (String) cpanelResult.get("error");
                        if (error != null && error.equals("User parameter is invalid or was not supplied")) {
                            // cpanel will still return a 200 return status even if there's an error,
                            //  so checking the error value is the next best way to detect this error.
                            throw new CpanelInvalidUserException("User parameter (" + username + ") is invalid or was not supplied");
                        }
                        throw e;
                    }

                    for (Object object : data) {
                        JSONObject addOnDomain = (JSONObject) object;
                        String domain = (String) addOnDomain.get("domain");
                        domains.add(domain);
                    }
                }

                domains.sort(Comparator.comparing(d -> d));
                return domains;
            } catch (ParseException | ClassCastException e) {
                throw new IOException("Error parsing cPanel account list response", e);
            }
        });
    }

    @Override
    public List<CPanelDomain> listDomains(long hfsVmId, CPanelDomainType type)
            throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> handleCpanelCall("listDomains", () -> cPanelClient.listDomains(type),
                dataJson -> {
                    JSONArray domainsJson = (JSONArray) dataJson.get("domains");
                    if (domainsJson != null) {
                        List<CPanelDomain> domains = new ArrayList<>();
                        for (Object object : domainsJson) {
                            JSONObject domainJson = (JSONObject) object;
                            CPanelDomain domain = new CPanelDomain();
                            domain.domainName = (String) domainJson.get("domain");
                            domain.domainType = (String) domainJson.get("domain_type");
                            domain.username = (String) domainJson.get("user");
                            domains.add(domain);
                        }
                        return domains;
                    }
                    throw new RuntimeException("WHM list domains by type failed: domains returned null");
                },
                reason -> {
                    throw new RuntimeException("WHM list domains by type failed due to reason: " + reason);
                })
        );
    }

    private JSONArray getResponseDataOrHandleException(JSONObject cPanelResult, String error, String username) {
        JSONArray data;
        try {
            data = (JSONArray) cPanelResult.get("data");
        } catch (ClassCastException e) {
            if (error != null && error.equals("User parameter is invalid or was not supplied")) {
                // cPanel will still return a 200 return status even if there's an error,
                // checking the error's value is the best way to detect this error.
                throw new CpanelInvalidUserException("The cPanel username (" + username + ") is either invalid or not supplied");
            }
            throw e;
        }
        return data;
    }

    @Override
    public String addAddOnDomain(long hfsVmId, String username, String newDomain) throws CpanelTimeoutException, CpanelAccessDeniedException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/cPanel+API+2+Functions+-+AddonDomain%3A%3Aaddaddondomain
            String responseJson = cPanelClient.addAddOnDomain(username, newDomain);
            String result = "0";
            String error;

            JSONArray data;
            JSONObject jsonObject;
            JSONObject cPanelResult;

            try {
                jsonObject = (JSONObject) new JSONParser().parse(responseJson);
                cPanelResult = (JSONObject) jsonObject.get("cpanelresult");
            } catch (ParseException | ClassCastException e) {
                throw new IOException("Error parsing cPanel account list response", e);
            }

            if (cPanelResult != null) {
                error = (String) cPanelResult.get("error");
                data = getResponseDataOrHandleException(cPanelResult, error, username);
                result = error == null ? String.valueOf(((JSONObject) data.get(0)).get("result")) : error;
            }
            return result;
        });
    }

    @Override
    public CPanelSession createSession(long hfsVmId, String username, CpanelServiceType serviceType)
            throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> cPanelClient.createSession(username, serviceType));
    }

    @Override
    public CpanelBuild installRpmPackage(long hfsVmId, String packageName)
            throws CpanelAccessDeniedException, CpanelTimeoutException {
        // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+package_manager_submit_actions
        return withAccessToken(hfsVmId, cPanelClient -> {
            return handleCpanelCall(
                    "installRpmPackage", () -> cPanelClient.installRpmPackage(packageName),
                    dataJson -> {
                        Long buildNumber = (Long) dataJson.get("build");
                        if (buildNumber != null) {
                            return new CpanelBuild(buildNumber, packageName);
                        }
                        throw new RuntimeException("WHM install rpm package failed: build returned null");

                    },
                    reason -> {
                        throw new RuntimeException("WHM install rpm package failed due to reason: " + reason);
                    });
        });
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
        return handleCpanelCall(callName, false, callFn, successHandler, errorHandler);
    }

    private <T> T handleCpanelCall(String callName, boolean hasNoResponseData, CpanelCall callFn, SuccessHandler<T> successHandler,
                                   ErrorHandler errorHandler) throws CpanelAccessDeniedException, IOException {

        try {
            String response = callFn.handle();
            logger.debug("Response for call [{}]: {}", callName, response);

            JSONParser parser = new JSONParser();
            JSONObject responseJson = (JSONObject) parser.parse(response);
            if (didCallSucceed(responseJson)) {
                if (hasNoResponseData) {
                    return successHandler.handle(responseJson);
                }
                JSONObject data = (JSONObject) responseJson.get("data");
                if (data != null) {
                    return successHandler.handle(data);
                }
            } else {
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
        return withAccessToken(hfsVmId, cPanelClient -> {
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
    public Void createAccount(long hfsVmId, String domainName, String username,
                              String password, String plan, String contactEmail)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+createacct
            return handleCpanelCall(
                    "createAccount",
                    () -> cPanelClient.createAccount(domainName, username, password, plan, contactEmail),
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
    public List<InstallatronApplication> listInstalledInstallatronApplications(long hfsVmId, String username) throws CpanelAccessDeniedException, CpanelTimeoutException {

        return withAccessToken(hfsVmId, cPanelClient -> {
            JSONParser parser = new JSONParser();
            String installedAppJson = cPanelClient.listInstalledInstallatronApplications(username);
            try {
                List<InstallatronApplication> installedApps = new ArrayList<>();
                JSONObject jsonObject = (JSONObject) parser.parse(installedAppJson);
                Boolean installatronResult = (Boolean) jsonObject.get("result");
                if (installatronResult == true) {
                    JSONArray data;
                    data = (JSONArray) jsonObject.get("data");
                    for (Object object : data) {
                        JSONObject installedApp = (JSONObject) object;
                        String appName = (String) installedApp.get("installer");
                        String id = (String) installedApp.get("id");
                        String version = (String) installedApp.get("version");
                        String domain = (String) installedApp.get("url");
                        String urlDomain = (String) installedApp.get("url-domain");
                        InstallatronApplication app = new InstallatronApplication(appName, id, domain, urlDomain, version);
                        installedApps.add(app);
                    }
                    return installedApps;
                }
                throw new RuntimeException("Error querying Installatron for list of installed applications due to reason: " + jsonObject.get("message"));
            } catch (ParseException | ClassCastException e) {
                throw new IOException("Error parsing list of Installatron installed applications", e);
            }
        });
    }

    @Override
    public Long getActiveBuilds(long hfsVmId, long buildNumber) throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+package_manager_is_performing_actions
            return handleCpanelCall(
                    "getRpmPackageUpdateStatus",
                    () -> cPanelClient.getRpmPackageUpdateStatus(Long.toString(buildNumber)),
                    dataJson -> {
                        Long activeCount = (Long) dataJson.get("active");
                        if (activeCount != null) {
                            return activeCount;
                        }
                        throw new RuntimeException("No active builds found - number of builds returned null");
                    },
                    reason -> {
                        throw new RuntimeException("WHM get Rpm Package Update Status failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public List<String> listInstalledRpmPackages(long hfsVmId) throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+list_rpms
            return handleCpanelCall(
                    "listInstalledRpms",
                    () -> cPanelClient.listInstalledRpmPackages(),
                    dataJson -> {
                        JSONArray pkgsJson = (JSONArray) dataJson.get("packages");
                        if (pkgsJson != null) {
                            List<String> packages = new ArrayList<>();
                            for (Object object : pkgsJson) {
                                JSONObject pkgJsonObj = (JSONObject) object;
                                String pkg = (String) pkgJsonObj.get("package");
                                if (pkg != null) {
                                    packages.add(pkg);
                                }
                            }

                            return packages;
                        }
                        throw new RuntimeException("No installed cpanel rpm packages present");
                    },
                    reason -> {
                        throw new RuntimeException("WHM list installed rpm packages failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public List<CPanelAccountCacheStatus> getNginxCacheConfig(long hfsVmId) throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+version
            return handleCpanelCall(
                    "getCacheConfig",
                    () -> cPanelClient.getNginxCacheConfig(),
                    dataJson -> {
                        JSONArray userConfigsJson = (JSONArray) dataJson.get("users");
                        if (userConfigsJson != null) {
                            List<CPanelAccountCacheStatus> userConfigs = new ArrayList<>();
                            for (Object object : userConfigsJson) {
                                JSONObject userConfigsObj = (JSONObject) object;
                                String username = (String) userConfigsObj.get("user");
                                JSONObject configObj = (JSONObject) userConfigsObj.get("config");
                                Boolean isEnabled = configObj != null ? (Boolean) configObj.get("enabled") : null;
                                if (username != null && isEnabled != null) {
                                    userConfigs.add(new CPanelAccountCacheStatus(username, isEnabled));
                                }
                            }
                            return userConfigs;
                        }
                        throw new RuntimeException("No nginx cache config found - users list returned null");
                    },
                    reason -> {
                        throw new RuntimeException("WHM get nginx cache config failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public String getVersion(long hfsVmId) throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://documentation.cpanel.net/display/DD/WHM+API+1+Functions+-+version
            return handleCpanelCall(
                    "getVersion",
                    () -> cPanelClient.getVersion(),
                    dataJson -> {
                        String version = (String) dataJson.get("version");
                        if (version != null) {
                            return version;
                        }
                        throw new RuntimeException("No version found - version data returned null");
                    },
                    reason -> {
                        throw new RuntimeException("WHM get Rpm version failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public List<String> listPackages(long hfsVmId) throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {
        return withAccessToken(hfsVmId, cPanelClient -> {
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

    @Override
    public String updateNginx(long hfsVmId, boolean enabled, List<String> usernames)
            throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://api.docs.cpanel.net/openapi/whm/operation/nginxmanager_set_cache_config/
            return handleCpanelCall(
                    "updateNginx", true,
                    () -> cPanelClient.updateNginx(enabled, usernames),
                    dataJson -> {
                        return null;
                    },
                    reason -> {
                        throw new RuntimeException("Update NGiNX failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public String clearNginxCache(long hfsVmId, List<String> usernames)
            throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://api.docs.cpanel.net/openapi/whm/operation/nginxmanager_clear_cache/
            return handleCpanelCall(
                    "clearNginxCache", true,
                    () -> cPanelClient.clearNginxCache(usernames),
                    dataJson -> {
                        return null;
                    },
                    reason -> {
                        throw new RuntimeException("Clear NGiNX cache failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public String getTweakSettings(long hfsVmId, String key) throws CpanelTimeoutException, CpanelAccessDeniedException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://api.docs.cpanel.net/openapi/whm/operation/get_tweaksetting/
            return handleCpanelCall(
                    "getTweakSettings",
                    () -> cPanelClient.getTweakSettings(key),
                    dataJson -> {
                        JSONObject tweakSettingJsonObj = (JSONObject) dataJson.get("tweaksetting");
                        return (String) tweakSettingJsonObj.get("value");
                    },
                    reason -> {
                        throw new RuntimeException("Get tweak settings failed due to reason: " + reason);
                    }
            );
        });
    }

    @Override
    public Void setTweakSettings(long hfsVmId, String key, String value) throws CpanelAccessDeniedException, CpanelTimeoutException {
        return withAccessToken(hfsVmId, cPanelClient -> {
            // https://api.docs.cpanel.net/openapi/whm/operation/set_tweaksetting/
            return handleCpanelCall(
                    "setTweakSettings", true,
                    () -> cPanelClient.setTweakSettings(key, value),
                    dataJson -> null,
                    reason -> {
                        throw new RuntimeException("Set tweak settings failed due to reason: " + reason);
                    }
            );
        });
    }
}