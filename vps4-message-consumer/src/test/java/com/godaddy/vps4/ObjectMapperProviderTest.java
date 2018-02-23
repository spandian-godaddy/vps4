package com.godaddy.vps4;

import com.godaddy.vps4.consumer.Vps4ConsumerInjector;
import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommService;
import org.junit.Assert;

public class ObjectMapperProviderTest {

    /**
     * This test really exists to ensure that the object mapper can pull up accounts.
     * The object mapper should have be able to pull up the account even if it has unknown properties.
     * If this flag is correctly set on the object mapper, then any updates to the Account object from hfs,
     * will not break the ecommService.
     * @throws Exception
     */
    //@Test
    public void testObjectMapperBinding() throws Exception {
        ECommService ecommService = Vps4ConsumerInjector.newInstance().getInstance(ECommService.class);
        Account account = ecommService.getAccount("42bfeedf-0df8-4973-88d4-6884b71dfaf8");
        Assert.assertNotNull(account);
    }
}
