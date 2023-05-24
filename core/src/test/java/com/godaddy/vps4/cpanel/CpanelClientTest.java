package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import com.godaddy.hfs.io.Charsets;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CpanelClientTest {
    private HttpClient httpClient;
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    String accessHash;
    CpanelClient cpanelClient;
    String hostname = "localhost";

    @Captor
    private ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor;

    @Before
    public void setUp() {
        accessHash = "FAKEACCESSHASH";
        response = mock(CloseableHttpResponse.class);
        httpClient = mock(HttpClient.class);
        try {
            ByteArrayEntity entity = new ByteArrayEntity("FOOBAR".getBytes(Charsets.UTF8));
            when(response.getEntity()).thenReturn(entity);
            when(response.getStatusLine())
                    .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
            when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        } catch (IOException e) {
            // the httpClient.execute call throws a checked exception IOException
        }

        cpanelClient = new CpanelClient(hostname, accessHash, httpClient);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void callsCpanelEndpointToCalculatePasswordStrength() throws CpanelAccessDeniedException, IOException {
        String password = "password";
        cpanelClient.calculatePasswordStrength(password);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/get_password_strength?api.version=1&password=" + password;
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToListAllDomains() throws CpanelAccessDeniedException, IOException {
        cpanelClient.listDomains(CPanelDomainType.ALL);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/get_domain_info?api.version=1"
                + "&api.filter.a.field=domain_type&api.filter.a.arg0=all&api.filter.a.type=eq&api.filter.enable=0"
                + "&api.sort.a.field=domain&api.sort.enable=1&api.columns.a=user"
                + "&api.columns.b=domain&api.columns.c=domain_type&api.columns.enable=1";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToListAddOnDomains() throws CpanelAccessDeniedException, IOException {
        cpanelClient.listDomains(CPanelDomainType.ADDON);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/get_domain_info?api.version=1"
                + "&api.filter.a.field=domain_type&api.filter.a.arg0=addon&api.filter.a.type=eq&api.filter.enable=1"
                + "&api.sort.a.field=domain&api.sort.enable=1&api.columns.a=user"
                + "&api.columns.b=domain&api.columns.c=domain_type&api.columns.enable=1";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToCreateAccount() throws CpanelAccessDeniedException, IOException {
        String domainName = "blah";
        String username = "blahtoo";
        String password = "password";
        String plan = "some fake plan";
        String email = "email@email.com";
        cpanelClient.createAccount(domainName, username, password, plan, email);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087" + "/json-api/createacct?api.version=1&password="
                + URLEncoder.encode(password, "UTF-8") +
                "&domain=" + URLEncoder.encode(domainName, "UTF-8")
                + "&username=" + URLEncoder.encode(username, "UTF-8")
                + "&plan=" + URLEncoder.encode(plan, "UTF-8")
                + "&contactemail=" + URLEncoder.encode(email, "UTF-8");
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToListPackages() throws CpanelAccessDeniedException, IOException {
        cpanelClient.listPackages();

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/listpkgs?api.version=1";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToCreateAddOnDomain() throws CpanelAccessDeniedException, IOException {
        String username = "vpsdev";
        String newDomain = "godaddy.com";
        String expectedUri = "https://" + hostname + ":2087/json-api/cpanel?api.version=1" +
                "&cpanel_jsonapi_user=vpsdev" +
                "&cpanel_jsonapi_apiversion=2" +
                "&cpanel_jsonapi_module=AddonDomain" +
                "&cpanel_jsonapi_func=addaddondomain" +
                "&dir=godaddy.com" +
                "&newdomain=godaddy.com" +
                "&subdomain=godaddy.com" +
                "&ftp_is_optional=false";

        cpanelClient.addAddOnDomain(username, newDomain);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToInstallRpmPackage() throws CpanelAccessDeniedException, IOException {
        cpanelClient.installRpmPackage("testPackage");

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/package_manager_submit_actions?api.version=1&install=testPackage";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToListRpmPackage() throws CpanelAccessDeniedException, IOException {
        cpanelClient.listInstalledRpmPackages();

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/package_manager_list_packages?api.version=1&state=installed";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToGetRpmPackageUpdateStatus() throws CpanelAccessDeniedException, IOException {
        cpanelClient.getRpmPackageUpdateStatus("12345");

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/package_manager_is_performing_actions?api.version=1&build=12345";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToGetVersion() throws CpanelAccessDeniedException, IOException {
        cpanelClient.getVersion();

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/version?api.version=1";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToGetNginxCacheConfig() throws CpanelAccessDeniedException, IOException {
        cpanelClient.getNginxCacheConfig();

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_get_cache_config_users?api.version=1&merge=1";
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToUpdateNginxTrue() throws CpanelAccessDeniedException, IOException {
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_set_cache_config?api.version=1&enabled=1";

        cpanelClient.updateNginx(true, null);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToUpdateNginxFalse() throws CpanelAccessDeniedException, IOException {
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_set_cache_config?api.version=1&enabled=0";

        cpanelClient.updateNginx(false, null);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToUpdateNginxWithUsernames() throws CpanelAccessDeniedException, IOException {
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_set_cache_config?api.version=1&enabled=1&user=vpsdev1&user=vpsdev2";
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");

        cpanelClient.updateNginx(true, usernames);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToClearNginxCache() throws CpanelAccessDeniedException, IOException {
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_clear_cache?api.version=1";

        cpanelClient.clearNginxCache(null);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToClearNginxCacheWithUsernames() throws CpanelAccessDeniedException, IOException {
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_clear_cache?api.version=1"
                + "&user=vpsdev1&user=vpsdev2";

        cpanelClient.clearNginxCache(usernames);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToGetTweakSettingsWithKey() throws CpanelAccessDeniedException, IOException {
        String key = "allowremotedomains";
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/get_tweaksetting?api.version=1&key=allowremotedomains";

        cpanelClient.getTweakSettings(key);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToSetTweakSettingsWithKeyAndValue() throws CpanelAccessDeniedException, IOException {
        String key = "allowremotedomains";
        String value = "1";
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/set_tweaksetting?api.version=1&key=allowremotedomains&value=1";

        cpanelClient.setTweakSettings(key, value);

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test(expected = CpanelAccessDeniedException.class)
    public void testAccessDenied() throws Exception {
        when(response.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 403, ""));
        new FakeCpanelModule().provideAccessHashService();
        cpanelClient.listSites();
    }

    @Test(expected = IOException.class)
    public void testConnectTimeout() throws Exception {

        CpanelClient client = new CpanelClient("192.168.254.254", "don'tneedone");

        // note: connect timeout throws org.apache.http.conn.ConnectTimeoutException
        //       but we really just categorize those all as IOException

        client.setTimeout(1); // one millisecond

        client.listSites();
    }
}
