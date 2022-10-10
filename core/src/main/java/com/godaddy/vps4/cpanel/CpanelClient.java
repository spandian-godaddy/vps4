package com.godaddy.vps4.cpanel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    private final String accessHash;

    private final HttpClient httpClient;

    private int timeout;

    public CpanelClient(String hostname, String accessHash) {
        this(hostname, accessHash, DefaultHttpClient.get());
    }

    public CpanelClient(String hostname, String accessHash, HttpClient httpClient) {
        baseUrl = "https://" + hostname + ":2087";
        this.accessHash = accessHash;
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
                .setHeader("Authorization", "WHM root:" + accessHash)
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
