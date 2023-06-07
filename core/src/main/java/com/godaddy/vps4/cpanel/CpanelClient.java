package com.godaddy.vps4.cpanel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.godaddy.vps4.util.DefaultHttpClient;

public class CpanelClient {

    private static final Logger logger = LoggerFactory.getLogger(CpanelClient.class);

    private static final int DEFAULT_TIMEOUT = 6000;

    private final String baseUrl;

    private final String apiToken;

    private final HttpClient httpClient;

    private int timeout;

    public CpanelClient(String hostname, String apiToken) {
        this(hostname, apiToken, DefaultHttpClient.get());
    }

    public CpanelClient(String hostname, String apiToken, HttpClient httpClient) {
        baseUrl = "https://" + hostname + ":2087";
        this.apiToken = apiToken;
        this.httpClient = httpClient;
        this.timeout = DEFAULT_TIMEOUT;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    protected RequestBuilder newCpanelRequest() {
        return RequestBuilder.get()
                .setConfig(RequestConfig.custom()

                        // wait 3 seconds to establish a TCP connection
                        .setConnectTimeout(timeout)

                        // wait 3 seconds for read/write operations
                        .setSocketTimeout(timeout)
                        .build())
                .setHeader("Authorization", "WHM root:" + apiToken)
                ;
    }

    public CPanelSession createSession(String user, CpanelServiceType service) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest get = newCpanelRequest()
                .setUri(baseUrl + "/json-api/create_user_session")
                .addParameter("api.version", "1")
                .addParameter("service", service.name())
                .addParameter("user", user)
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(get)) {
            ensureAuthSuccess(response);

            response.getEntity().writeTo(baos);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        CPanelSession cPanelSession = mapper.readValue(baos.toString(), CPanelSession.class);

        return cPanelSession;
    }

    public String listSites() throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/listaccts")
                .addParameter("api.version", "1")
                .build();
        return callWhm(request);
    }

    public String installRpmPackage(String packageName) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/package_manager_submit_actions")
                .addParameter("api.version", "1")
                .addParameter("install", packageName)
                .build();
        return callWhm(request);
    }

    public String listInstalledRpmPackages() throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/package_manager_list_packages")
                .addParameter("api.version", "1")
                .addParameter("state", "installed")
                .build();
        return callWhm(request);
    }

    public String getNginxCacheConfig() throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/nginxmanager_get_cache_config_users")
                .addParameter("api.version", "1")
                .addParameter("merge", "1")
                .build();
        return callWhm(request);
    }

    public String getRpmPackageUpdateStatus(String buildNumber) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/package_manager_is_performing_actions")
                .addParameter("api.version", "1")
                .addParameter("build", buildNumber)
                .build();
        return callWhm(request);
    }

    public String listAddOnDomains(String user) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/cpanel")
                .addParameter("api.version", "1")
                .addParameter("cpanel_jsonapi_user", user)
                .addParameter("cpanel_jsonapi_apiversion", "2")
                .addParameter("cpanel_jsonapi_module", "AddonDomain")
                .addParameter("cpanel_jsonapi_func", "listaddondomains")
                .build();
        return callWhm(request);
    }

    public String listDomains(CPanelDomainType domainType) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/get_domain_info")
                .addParameter("api.version", "1")
                .addParameter("api.filter.a.field", "domain_type")
                .addParameter("api.filter.a.arg0", domainType.toString().toLowerCase())
                .addParameter("api.filter.a.type", "eq")
                .addParameter("api.filter.enable", domainType == domainType.ALL ? "0" : "1")
                .addParameter("api.sort.a.field", "domain")
                .addParameter("api.sort.enable", "1")
                .addParameter("api.columns.a", "user")
                .addParameter("api.columns.b", "domain")
                .addParameter("api.columns.c", "domain_type")
                .addParameter("api.columns.enable", "1")
                .build();
        return callWhm(request);
    }

    public String addAddOnDomain(String username, String newDomain) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/cpanel")
                .addParameter("api.version", "1")
                .addParameter("cpanel_jsonapi_user", username)
                .addParameter("cpanel_jsonapi_apiversion", "2")
                .addParameter("cpanel_jsonapi_module", "AddonDomain")
                .addParameter("cpanel_jsonapi_func", "addaddondomain")
                .addParameter("dir", newDomain)
                .addParameter("newdomain", newDomain)
                .addParameter("subdomain", newDomain)
                .addParameter("ftp_is_optional", String.valueOf(false))
                .build();
        return callWhm(request);
    }

    public String listInstalledInstallatronApplications(String user) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/installatron/index.cgi")
                .addParameter("api", "json")
                .addParameter("cmd", "installs")
                .addParameter("user", user)
                .build();
        return callWhm(request);
    }

    public String calculatePasswordStrength(String password) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/get_password_strength")
                .addParameter("api.version", "1")
                .addParameter("password", password)
                .build();
        return callWhm(request);
    }

    public String createAccount(String domainName, String username, String password, String plan, String contactEmail)
        throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/createacct")
                .addParameter("api.version", "1")
                .addParameter("password", password)
                .addParameter("domain", domainName)
                .addParameter("username", username)
                .addParameter("plan", plan)
                .addParameter("contactemail", contactEmail)
                .build();
        return callWhm(request);
    }

    public String listPackages() throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/listpkgs")
                .addParameter("api.version", "1")
                .build();
        return callWhm(request);
    }

    public String getVersion() throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/version")
                .addParameter("api.version", "1")
                .build();
        return callWhm(request);
    }

    public String updateNginx(boolean enabled, List<String> usernames) throws CpanelAccessDeniedException, IOException {
        RequestBuilder request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/nginxmanager_set_cache_config")
                .addParameter("api.version", "1")
                .addParameter("enabled", enabled ? "1" : "0");
        if (usernames != null && !usernames.isEmpty())
            for (String username : usernames) request.addParameter("user", username);

        return callWhm(request.build());
    }

    public String clearNginxCache(List<String> usernames) throws CpanelAccessDeniedException, IOException {
        RequestBuilder request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/nginxmanager_clear_cache")
                .addParameter("api.version", "1");
        if (usernames != null && !usernames.isEmpty())
            for (String username : usernames) request.addParameter("user", username);

        return callWhm(request.build());
    }

    public String getTweakSettings(String key) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/get_tweaksetting")
                .addParameter("api.version", "1")
                .addParameter("key", key)
                .build();
        return callWhm(request);
    }

    public String setTweakSettings(String key, String value) throws CpanelAccessDeniedException, IOException {
        HttpUriRequest request = newCpanelRequest()
                .setUri(baseUrl + "/json-api/set_tweaksetting")
                .addParameter("api.version", "1")
                .addParameter("key", key)
                .addParameter("value", value)
                .build();
        return callWhm(request);
    }

    private String callWhm(HttpUriRequest request) throws CpanelAccessDeniedException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {

            ensureAuthSuccess(response);

            response.getEntity().writeTo(baos);
        }
        return baos.toString("UTF-8");
    }

    protected void ensureAuthSuccess(CloseableHttpResponse response)
            throws CpanelAccessDeniedException {

        // cPanel access denied response looks like:
        // status: 403
        // {"cpanelresult":{"apiversion":"2","error":"Access denied","data":{"reason":"Access denied","result":"0"},"type":"text"}}
        if (response.getStatusLine().getStatusCode() == 403) {
            logger.warn("Access denied from cPanel server {}", this.baseUrl);
            throw new CpanelAccessDeniedException("Access denied from server " + this.baseUrl);
        }
    }

    public enum CpanelServiceType {
        cpaneld, whostmgrd, webmaild
    }
}
