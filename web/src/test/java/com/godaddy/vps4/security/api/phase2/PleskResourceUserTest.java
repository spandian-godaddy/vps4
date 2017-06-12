package com.godaddy.vps4.security.api.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.hfs.HfsMockModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.plesk.Vps4PleskService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.controlPanel.plesk.PleskResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.vhfs.vm.VmService;


public class PleskResourceUserTest {

    @Inject
    PrivilegeService privilegeService;

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    ActionService actionService;

    @Inject
    Vps4UserService userService;

    @Inject
    Vps4PleskService pleskService;

    @Inject
    ProjectService projService;

    @Inject
    VmService hfsVmService;

    Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new HfsMockModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    Vps4PleskService cpServ = Mockito.mock(Vps4PleskService.class);
                    bind(Vps4PleskService.class).toInstance(cpServ);

                }

                @Provides
                Vps4User provideUser() {
                    return user;
                }
            });

    Vps4User validUser;
    Vps4User invalidUser;
    Vps4User user;

    UUID vmId;
    UUID windowsVmId;
    UUID orionGuid;
    UUID centOrionGuid;
    long hfsVmId = 98765;
    long centHfsVmId = 56789;
    Project project;
    VirtualMachine virtualMachine;
    VirtualMachine centVirtualMachine;
    DataSource dataSource = injector.getInstance(DataSource.class);

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        centOrionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        project = projService.createProject("TestProject", validUser.getId(), "vps4-test-");
        createPleskVm();
        createWindowsVm();
    }

    private void createPleskVm() {
        virtualMachine = SqlTestData.insertTestVm(orionGuid, validUser.getId(), dataSource, "windows-2012r2-plesk-12.5");
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        vmId = virtualMachine.vmId;
    }

    private void createWindowsVm() {
        centVirtualMachine = SqlTestData.insertTestVm(centOrionGuid, validUser.getId(), dataSource, "windows-2012r2");
        virtualMachineService.addHfsVmIdToVirtualMachine(centVirtualMachine.vmId, centHfsVmId);
        windowsVmId = centVirtualMachine.vmId;
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(centVirtualMachine.vmId, dataSource);
    }

    private PleskResource getValidResource() {
        user = validUser;
        return injector.getInstance(PleskResource.class);
    }

    private PleskResource getInvalidResource() {
        user = invalidUser;
        return injector.getInstance(PleskResource.class);
    }

    @Test
    public void testGetPleskSession(){
        getValidResource().getPleskSessionUrl(vmId, "1.2.3.4", null, null);
    }

    @Test(expected=AuthorizationException.class)
    public void testPleskSessionInvalidUser(){
        getInvalidResource().getPleskSessionUrl(vmId, "1.2.3.4", null, null);
    }

    @Test(expected=Vps4Exception.class)
    public void testGetPleskSessionInvalidImage(){
        getValidResource().getPleskSessionUrl(windowsVmId, "1.2.3.4", null, null);
    }


    @Test(expected=AuthorizationException.class)
    public void testGetPleskSessionInvalidUser(){
        getInvalidResource().getPleskSessionUrl(windowsVmId, "1.2.3.4", null, null);
    }


    @Test
    public void testGetPleskAccounts(){
        getValidResource().listPleskAccounts(vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testPleskAccountsInvalidUser(){
        getInvalidResource().listPleskAccounts(vmId);
    }

    @Test(expected=Vps4Exception.class)
    public void testGetPleskAccountsInvalidImage(){
        getValidResource().listPleskAccounts(windowsVmId);
    }


    @Test(expected=AuthorizationException.class)
    public void testGetPleskAccountsInvalidUser(){
        getInvalidResource().listPleskAccounts(windowsVmId);
    }


}
