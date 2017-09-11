package com.godaddy.vps4;

import com.godaddy.vps4.consumer.Vps4ConsumerInjector;
import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommService;
import org.junit.Assert;
import org.junit.Test;

public class ObjectMapperProviderTest {
    

    @Test
    public void testObjectMapperBinding() throws Exception {
        ECommService ecommService = Vps4ConsumerInjector.newInstance().getInstance(ECommService.class);
        Account account = ecommService.getAccount("42bfeedf-0df8-4973-88d4-6884b71dfaf8");
        Assert.assertNotNull(account);
    }
}
