package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.credit.CreditResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class CreditResourceTest {

    private Vps4User validUser;
    private Vps4User invalidUser;
    private Vps4User user;
    private UUID orionGuid = UUID.randomUUID();

    private Injector injector = Guice.createInjector(
            new DatabaseModule(), 
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {
                
                @Override
                public void configure() {
                }

                @Provides
                public Vps4User provideUser() {
                    return user;
                }
            });

    Vps4UserService userService = injector.getInstance(Vps4UserService.class);
    VirtualMachineService virtualMachineService = injector.getInstance(VirtualMachineService.class);

    protected CreditResource newValidCreditResource() {
        user = validUser;
        return injector.getInstance(CreditResource.class);
    }

    protected CreditResource newInvalidCreditResource() {
        user = invalidUser;
        return injector.getInstance(CreditResource.class);
    }

    @Before
    public void setupTest() {
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "cPanel", 10, 1, "validUserShopperId");
    }

    @After
    public void teardownTest() {
        DataSource dataSource = injector.getInstance(DataSource.class);
        Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, orionGuid);
    }

    @Test
    public void testGetVmRequest() {

        VirtualMachineCredit credit = newValidCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);
        try {
            newInvalidCreditResource().getCredit(orionGuid);
            Assert.fail();
        }
        catch (IllegalArgumentException e) {
            // do nothing
        }
    }

}
