package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.time.Instant;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import com.godaddy.hfs.io.Charsets;

import static org.mockito.Mockito.*;

public class CpanelClientTest {

    @Test(expected=CpanelAccessDeniedException.class)
    public void testAccessDenied() throws Exception {

        HttpClient httpClient = mock(HttpClient.class);

        // response
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        ByteArrayEntity entity = new ByteArrayEntity(
                "{\"cpanelresult\":{\"apiversion\":\"2\",\"error\":\"Access denied\",\"data\":{\"reason\":\"Access denied\",\"result\":\"0\"},\"type\":\"text\"}}".getBytes(Charsets.UTF8));
        when(response.getEntity()).thenReturn(entity);

        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 403, ""));

        // wire execution
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);

        CpanelAccessHashService accessHashService = new FakeCpanelModule()
                .provideAccessHashService();

        CpanelClient client = new CpanelClient(
                "50.62.9.38",
                accessHashService.getAccessHash(
                        1, "", "", Instant.now().plusSeconds(2)),
                httpClient);

        // should throw a CpanelAccessDeniedException
        client.listSites();
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
