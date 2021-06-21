package com.godaddy.vps4.shopperNotes;

import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import javax.xml.soap.SOAPMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;

@RunWith(MockitoJUnitRunner.class)
public class DefaultShopperNotesClientServiceTest {

    private DefaultShopperNotesClientService service;
    private ShopperNoteRequest request;

    @Mock private Config config;

    @Before
    public void setUp() throws Exception {
        setupRequest();
        setUpMocks();
        service = new DefaultShopperNotesClientService(config);
    }

    private void setupRequest() {
        request = new ShopperNoteRequest();
        request.plId = "1234";
        request.shopperId = "fake-shopper";
        request.enteredBy = "vps4-test-name";
        request.shopperNote = "Test shopper note";
        request.requestingIp = "127.0.0.1";
        request.enteredDateTime = "06/09/2021 18:00:00";
    }

    private void setUpMocks() {
        when(config.get("shopper.notes.api.url")).thenReturn("https://fakedomain.godaddy.com");
    }

    private String soapMessageToString(SOAPMessage message) throws Exception {
        Method toStringMethod = service.getClass().getDeclaredMethod("soapMessageToString", SOAPMessage.class);
        toStringMethod.setAccessible(true);
        return (String) toStringMethod.invoke(service, message);
    }

    @Test
    public void testBuildSoapMessage() throws Exception {
        Method buildMethod = service.getClass().getDeclaredMethod("buildSoapRequest", ShopperNoteRequest.class);
        buildMethod.setAccessible(true);
        SOAPMessage result = (SOAPMessage) buildMethod.invoke(service, request);
        String expected = "<SOAP-ENV:Envelope " +
                "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:SOAP-URN=\"https://fakedomain.godaddy.com\">" +
                "<SOAP-ENV:Header/>" +
                "<SOAP-ENV:Body>" +
                "<SOAP-URN:ProcessShopperMessage>" +
                "<SOAP-URN:bstrMessageXml>" +
                "<messageXml namespace=\"Notes\" type=\"ShopperNote\">" +
                "<dictionary>" +
                "<item name=\"PrivateLabelID\">1234</item>" +
                "<item name=\"ShopperID\">fake-shopper</item>" +
                "<item name=\"EnteredBy\">vps4-test-name</item>" +
                "<item name=\"RequestingIP\">127.0.0.1</item>" +
                "<item name=\"ShopperNote\">Test shopper note</item>" +
                "<item name=\"EnteredDate\">06/09/2021 18:00:00</item>" +
                "</dictionary>" +
                "</messageXml>" +
                "</SOAP-URN:bstrMessageXml>" +
                "</SOAP-URN:ProcessShopperMessage>" +
                "</SOAP-ENV:Body>" +
                "</SOAP-ENV:Envelope>";
        Assert.assertEquals(expected, soapMessageToString(result));
    }
}
