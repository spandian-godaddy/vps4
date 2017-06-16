package com.godaddy.vps4.cpanel;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class CPanelSessionTest {
    @Test
    public void testMetaDataProperties() {
        CPanelSession cPanelSession = new CPanelSession();
        CPanelSession.MetaData metaData = cPanelSession.new MetaData();
        Random randomInt = new Random();
        int result = randomInt.nextInt(1000);
        metaData.result = result;
        int version = randomInt.nextInt(1000);
        metaData.version = version;
        String reason = UUID.randomUUID().toString();
        metaData.reason = reason;
        String command = UUID.randomUUID().toString();
        metaData.command = command;

        Assert.assertEquals(result, metaData.result);
        Assert.assertEquals(version, metaData.version);
        Assert.assertEquals(reason, metaData.reason);
        Assert.assertEquals(command, metaData.command);
    }

    @Test
    public void testDataProperties() {
        CPanelSession cPanelSession = new CPanelSession();
        CPanelSession.Data data = cPanelSession.new Data();
        String cpSecurityToken = UUID.randomUUID().toString();
        data.cpSecurityToken = cpSecurityToken;
        String service = UUID.randomUUID().toString();
        data.service = service;
        String session = UUID.randomUUID().toString();
        data.session = session;
        String url = UUID.randomUUID().toString();
        data.url = url;

        Assert.assertEquals(cpSecurityToken, data.cpSecurityToken);
        Assert.assertEquals(service, data.service);
        Assert.assertEquals(session, data.session);
        Assert.assertEquals(url, data.url);
    }

    @Test
    public void testDataGetAndSetExpires() {
        CPanelSession cPanelSession = new CPanelSession();
        CPanelSession.Data data = cPanelSession.new Data();
        Random random = new Random();
        int epochSeconds = random.nextInt(1000);
        data.setExpires(epochSeconds);
        Instant expectedExpires = Instant.ofEpochSecond(epochSeconds);

        Assert.assertEquals(expectedExpires, data.getExpires());
    }
}
