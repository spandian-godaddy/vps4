package com.godaddy.vps4.shopperNotes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.google.inject.Inject;

public class DefaultShopperNotesClientService implements ShopperNotesClientService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultShopperNotesClientService.class);
    private final String apiUrl;
    private final HttpsURLConnection connection;

    @Inject
    public DefaultShopperNotesClientService(Config config) {
        apiUrl = config.get("shopper.notes.api.url", null);
        connection = ( apiUrl == null ) ? null : buildConnection();
    }

    private HttpsURLConnection buildConnection() {
        try {
            return (HttpsURLConnection) new URL(apiUrl).openConnection();
        } catch (IOException e) {
            logger.error("Exception creating shopper message connection: " + e);
            throw new RuntimeException(e);
        }
    }


    private SOAPConnection connect() throws IOException, SOAPException {
        connection.connect();
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        return soapConnectionFactory.createConnection();
    }

    private void disconnect(SOAPConnection soapConnection) throws SOAPException {
        soapConnection.close();
        connection.disconnect();
    }

    private String soapMessageToString(SOAPMessage message) throws IOException, SOAPException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString();
    }

    @Override
    public UUID processShopperMessage(ShopperNoteRequest request) {
        try {
            SOAPMessage soapRequest = buildSoapRequest(request);
            logger.info("Request to shopper notes API: {}", soapMessageToString(soapRequest));

            SOAPConnection soapConnection = connect();
            SOAPMessage soapResponse = soapConnection.call(soapRequest, apiUrl);
            disconnect(soapConnection);
            logger.info("Response from shopper notes API: {}", soapMessageToString(soapResponse));
            return UUID.fromString(soapResponse.getSOAPBody().getTextContent().trim()
                                               .replace("<RESPONSE><SUCCESS>", "")
                                               .replace("</SUCCESS></RESPONSE>", ""));
        } catch (SOAPException | IOException e) {
            logger.error("Exception processing shopper message: " + e);
            throw new RuntimeException(e);
        }
    }

    private SOAPMessage buildSoapRequest(ShopperNoteRequest request) throws IOException, SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapRequest = messageFactory.createMessage();
        SOAPPart soapPart = soapRequest.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("SOAP-URN", apiUrl);

        SOAPBody soapBody = envelope.getBody();
        buildSoapBody(soapBody, request);

        soapRequest.saveChanges();
        return soapRequest;
    }

    private void buildSoapBody(SOAPBody soapBody, ShopperNoteRequest request) throws SOAPException {
        SOAPElement xml = soapBody.addChildElement("ProcessShopperMessage", "SOAP-URN")
                                  .addChildElement("bstrMessageXml", "SOAP-URN")
                                  .addChildElement("messageXml")
                                  .addAttribute(new QName("namespace"), "Notes")
                                  .addAttribute(new QName("type"), "ShopperNote");
        SOAPElement dictionary = xml.addChildElement("dictionary");
        dictionary.addChildElement("item")
                  .addAttribute(new QName("name"), "PrivateLabelID")
                  .addTextNode(request.plId);
        dictionary.addChildElement("item")
                  .addAttribute(new QName("name"), "ShopperID")
                  .addTextNode(request.shopperId);
        dictionary.addChildElement("item")
                  .addAttribute(new QName("name"), "EnteredBy")
                  .addTextNode(request.enteredBy);
        dictionary.addChildElement("item")
                  .addAttribute(new QName("name"), "RequestingIP")
                  .addTextNode(request.requestingIp);
        dictionary.addChildElement("item")
                  .addAttribute(new QName("name"), "ShopperNote")
                  .addTextNode(request.shopperNote);
        dictionary.addChildElement("item")
                  .addAttribute(new QName("name"), "EnteredDate")
                  .addTextNode(request.enteredDateTime);
    }
}
