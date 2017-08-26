package com.godaddy.vps4.util;
import com.godaddy.hfs.config.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.config.Configs;
import com.godaddy.vps4.messaging.DefaultVps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.Assert;

import java.util.UUID;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecureHttpClientTest {

    @Test
    public void testCreateHttpClient() {
        Config config = setupConfig();
        CloseableHttpClient client = SecureHttpClient.createHttpClient(
                config,
                DefaultVps4MessagingService.CLIENT_CERTIFICATE_KEY_PATH,
                DefaultVps4MessagingService.CLIENT_CERTIFICATE_PATH);
        Assert.assertNotNull(client);
    }

    private Config setupConfig() {
        Config config = mock(Config.class);
        byte[] fakeByteArray = new byte[] { (byte) 129, (byte) 130, (byte) 131};
        when(config.getData(anyString())).thenReturn(fakeByteArray);

        return config;
    }

    @Test
    public void testConstructor() {
        Config config = setupConfig();
        SecureHttpClient client = new SecureHttpClient(
                config,
                DefaultVps4MessagingService.CLIENT_CERTIFICATE_KEY_PATH,
                DefaultVps4MessagingService.CLIENT_CERTIFICATE_PATH);
        Assert.assertNotNull(client);
    }

    @Test
    public void testCreateJSONFromObject() {
        Message message = new Message();
        message.messageId = UUID.randomUUID().toString();
        message.status = UUID.randomUUID().toString();
        message.shopperId = UUID.randomUUID().toString();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String expectedJson = objectMapper.writeValueAsString(message);
            String actualJson = SecureHttpClient.createJSONFromObject(message);

            Assert.assertEquals(expectedJson, actualJson);
        }
        catch (JsonProcessingException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testCreateJsonHttpGetContentType() {
        String uri = UUID.randomUUID().toString();
        HttpGet actualHttpGet = SecureHttpClient.createJsonHttpGet(uri);

        Assert.assertEquals("application/json", actualHttpGet.getHeaders("Content-type")[0].getValue());
    }

    @Test
    public void testCreateJsonHttpGetAccept() {
        String uri = UUID.randomUUID().toString();
        HttpGet actualHttpGet = SecureHttpClient.createJsonHttpGet(uri);

        Assert.assertEquals("application/json", actualHttpGet.getHeaders("Accept")[0].getValue());
    }

    @Test
    public void testSetDefaultJsonHeaders() {
        Header[] headers = SecureHttpClient.getDefaultJsonHeaders();
        Assert.assertTrue(Arrays.stream(headers).anyMatch(h -> h.getName() == "Content-type" &&
                h.getValue() == "application/json"));
        Assert.assertTrue(Arrays.stream(headers).anyMatch(h -> h.getName() == "Accept" &&
                h.getValue() == "application/json"));
    }

    @Test
    public void testCreateJsonHttpPostWithHeaders() {
        String uri = UUID.randomUUID().toString();
        Map<String, String> headersToAdd = new HashMap<String, String>();
        String header1 = UUID.randomUUID().toString();
        String header2 = UUID.randomUUID().toString();
        String header3 = UUID.randomUUID().toString();
        headersToAdd.put("1", header1);
        headersToAdd.put("2", header2);
        headersToAdd.put("3", header3);
        HttpPost actualHttpPost = SecureHttpClient.createJsonHttpPostWithHeaders(uri, headersToAdd);

        Assert.assertEquals("application/json", actualHttpPost.getHeaders("Content-type")[0].getValue());
        Assert.assertEquals("application/json", actualHttpPost.getHeaders("Accept")[0].getValue());
        Assert.assertEquals(header1, actualHttpPost.getHeaders("1")[0].getValue());
        Assert.assertEquals(header2, actualHttpPost.getHeaders("2")[0].getValue());
        Assert.assertEquals(header3, actualHttpPost.getHeaders("3")[0].getValue());
    }
}
