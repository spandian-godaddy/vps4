package com.godaddy.vps4.cpanel;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CPanelAccountTest {
    @Test
    public void testCPanelAccountProperties() {
        String name = UUID.randomUUID().toString();
        String username = UUID.randomUUID().toString();
        CPanelAccount targetCPanelAccount = new CPanelAccount(name, username);

        Assert.assertEquals(name, targetCPanelAccount.name);
        Assert.assertEquals(username, targetCPanelAccount.username);
    }
}
