package com.godaddy.vps4.web.vm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.project.UserProjectPrivilege;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmShopperMergeResourceTest {

    Vps4UserService userService = mock(Vps4UserService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    PrivilegeService privilegeService = mock(PrivilegeService.class);
    CreditService creditService = mock(CreditService.class);
    VmResource vmResource = mock(VmResource.class);
    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    VirtualMachine vm = new VirtualMachine();
    VirtualMachineCredit shopperCredit;
    VirtualMachineCredit notShopperCredit;
    VmShopperMergeResource testShopperMergeResource;
    UserProjectPrivilege userProjectPrivilege = mock(UserProjectPrivilege.class);
    Vps4User newVps4User;
    String resellerId = "foobar";

    private GDUser newUser;
    private GDUser oldUser;
    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);

    private Injector injector = Guice.createInjector(
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(Vps4UserService.class).toInstance(userService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                    bind(PrivilegeService.class).toInstance(privilegeService);
                    bind(CreditService.class).toInstance(creditService);
                    bind(VmResource.class).toInstance(vmResource);
                }

                @Provides
                public GDUser provideUser() {
                    return newUser;
                }
            });

    @Before
    public void setupTest() {
        newUser = GDUserMock.createShopper();

        userProjectPrivilege.vps4UserId = 2;
        newVps4User = new Vps4User(1, newUser.getShopperId());
        shopperCredit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID(GDUserMock.DEFAULT_SHOPPER)
                .withResellerID(resellerId)
                .build();
        notShopperCredit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID("shopper2")
                .withResellerID(resellerId)
                .build();
        vm.orionGuid = orionGuid;
        when(vmResource.getVm(vmId)).thenReturn(vm);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(shopperCredit);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        when(privilegeService.getActivePrivilege(virtualMachineService.getVirtualMachine(vmId).projectId))
                .thenReturn(userProjectPrivilege);
        when(userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, resellerId)).thenReturn(newVps4User);
        testShopperMergeResource = getVmShopperMergeResource();

    }

    private VmShopperMergeResource getVmShopperMergeResource() {
        return injector.getInstance(VmShopperMergeResource.class);
    }

    private VmShopperMergeResource.ShopperMergeRequest createVmShopperMergeRequest(String shopperId) {
        VmShopperMergeResource.ShopperMergeRequest request = new VmShopperMergeResource.ShopperMergeRequest();
        request.newShopperId = shopperId;
        return request;
    }

    @Test
    public void looksupToSeeIfVmExists() {
        oldUser = GDUserMock.createShopper();
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));

        verify(vmResource, times(1)).getVm(vmId);
    }

    @Test
    public void makeSureNewShopperOwnsTheCredit() {
        try {
            testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));
        } catch (RuntimeException ex) {
            fail("This should never fail");

        }
    }

    @Test(expected = AuthorizationException.class)
    public void ifNewShopperDoesNotOwnCreditThrowException() {
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(notShopperCredit);
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));
    }

    @Test
    public void getOrCreateUserIfDoesNotExist() {
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));
        
        verify(userService, times(1)).getOrCreateUserForShopper(newUser.getShopperId(), resellerId);
    }

    @Test(expected = IllegalStateException.class)
    public void ifUserCreationFails() {
        when(userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, resellerId)).thenThrow(
                new IllegalStateException("Unable to lazily create user for shopper " + GDUserMock.DEFAULT_SHOPPER));
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));
    }

    @Test
    public void getCurrentActivePrivilege() {
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));

        verify(privilegeService, times(1)).getActivePrivilege(virtualMachineService.getVirtualMachine(vmId).projectId);
    }

    @Test
    public void outdatesCurrentPrivilege() {
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));

        verify(privilegeService, times(1))
                .outdateVmPrivilegeForShopper(userProjectPrivilege.vps4UserId, userProjectPrivilege.projectId);
    }

    @Test
    public void createNewPrivilegeForUser() {
        testShopperMergeResource.mergeTwoShopperAccounts(vmId, createVmShopperMergeRequest(newUser.getShopperId()));
        
        verify(privilegeService, times(1)).addPrivilegeForUser(newVps4User.getId(), userProjectPrivilege.privilegeId,
                userProjectPrivilege.projectId);
    }

}
