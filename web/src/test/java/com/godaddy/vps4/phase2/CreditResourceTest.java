package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class CreditResourceTest {

    private GDUser validUser;
    private GDUser invalidUser;
    private GDUser user;
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
                public GDUser provideUser() {
                    return user;
                }
            });

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
        validUser = GDUserMock.createShopper("validUserShopperId");
        invalidUser = GDUserMock.createShopper("invalidUserShopperId");
        vmCredit = new VirtualMachineCredit(orionGuid, 10, 1, 0, "linux", "cPanel",
                null, null, "validUserShopperId", AccountStatus.ACTIVE, null, null);
        Mockito.when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
    }

    @Test
    public void testGetCredit() {
        VirtualMachineCredit credit = newValidCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);
    }

    @Test(expected=NotFoundException.class)
    public void testGetUnauthorizedCredit() {
        newInvalidCreditResource().getCredit(orionGuid);
    }

}
