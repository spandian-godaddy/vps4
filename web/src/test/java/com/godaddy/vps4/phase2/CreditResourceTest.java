package com.godaddy.vps4.phase2;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.AccountStatus;
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
    private VirtualMachineCredit vmCredit;
    CreditService creditService = Mockito.mock(CreditService.class);


    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CreditService.class).toInstance(creditService);
                }

                @Provides
                public Vps4User provideUser() {
                    return user;
                }
            });

    Vps4UserService userService = injector.getInstance(Vps4UserService.class);

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
        vmCredit = new VirtualMachineCredit(orionGuid, 10, 1, 0, "linux", "cPanel",
                null, null, "validUserShopperId", AccountStatus.ACTIVE, null);
        Mockito.when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
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
