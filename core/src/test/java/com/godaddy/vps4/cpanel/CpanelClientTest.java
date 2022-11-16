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

        cpanelClient.updateNginx(true, "");

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToUpdateNginxFalse() throws CpanelAccessDeniedException, IOException {
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_set_cache_config?api.version=1&enabled=0";

        cpanelClient.updateNginx(false, "");

        verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
        HttpUriRequest capturedReq = httpUriRequestArgumentCaptor.getValue();
        Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
    }

    @Test
    public void callsCpanelEndpointToUpdateNginxWithUsername() throws CpanelAccessDeniedException, IOException {
        String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/nginxmanager_set_cache_config?api.version=1&enabled=1&user=vpsdev";

        cpanelClient.updateNginx(true, "vpsdev");

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
