package com.godaddy.vps4.cpanel;

import java.io.IOException;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.io.Charsets;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class CpanelClientTest {
    private HttpClient httpClient;
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    String accessHash;
    CpanelClient cpanelClient;
    String hostname = "localhost";

    @Captor private ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor;

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
        }
        catch (IOException e) {
            // the httpClient.execute call throws a checked exception IOException
        }

        cpanelClient = new CpanelClient(hostname, accessHash, httpClient);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void callsCpanelEndpointToCalculatePasswordStrength() {
        String password = "password";
        try {
            cpanelClient.calculatePasswordStrength(password);

            verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
            HttpUriRequest capturedReq =  httpUriRequestArgumentCaptor.getValue();
            String expectedUri = "https://" + hostname + ":2087"
                + "/json-api/get_password_strength?api.version=1&password=" + password;
            Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
        }
        catch (CpanelAccessDeniedException | IOException e) {
            Assert.fail("This test shouldn't be failing!!");
        }
    }

    @Test
    public void callsCpanelEndpointToCreateAccount() {
        String domainName = "blah";
        String username = "blahtoo";
        String password = "password";
        String plan = "what";
        try {
            cpanelClient.createAccount(domainName, username, password, plan);

            verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
            HttpUriRequest capturedReq =  httpUriRequestArgumentCaptor.getValue();
            String expectedUri = "https://" + hostname + ":2087" + "/json-api/createacct?api.version=1&password="
                    + password + "&domain=" + domainName + "&username=" + username + "&plan=" + plan;
            Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
        }
        catch (CpanelAccessDeniedException | IOException e) {
            Assert.fail("This test shouldn't be failing!!");
        }
    }

    @Test
    public void callsCpanelEndpointToListPackages() {
        try {
            cpanelClient.listPackages();

            verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture());
            HttpUriRequest capturedReq =  httpUriRequestArgumentCaptor.getValue();
            String expectedUri = "https://" + hostname + ":2087"
                    + "/json-api/listpkgs?api.version=1";
            Assert.assertEquals(expectedUri, capturedReq.getURI().toString());
        }
        catch (CpanelAccessDeniedException | IOException e) {
            Assert.fail("This test shouldn't be failing!!");
        }
    }

    @Test(expected=CpanelAccessDeniedException.class)
    public void testAccessDenied() throws Exception {
        when(response.getStatusLine())
            .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 403, ""));
        new FakeCpanelModule().provideAccessHashService();
        cpanelClient.listSites();
    }

    @Test(expected=IOException.class)
    public void testConnectTimeout() throws Exception {

        CpanelClient client = new CpanelClient("192.168.254.254", "don'tneedone");

        // note: connect timeout throws org.apache.http.conn.ConnectTimeoutException
        //       but we really just categorize those all as IOException

        client.setTimeout(1); // one millisecond

        client.listSites();
    }

}
