package com.godaddy.vps4.security.api.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.hfs.HfsMockModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
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
import com.godaddy.vps4.web.controlPanel.cpanel.CPanelResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.vhfs.vm.VmService;

public class CpanelResourceUserTest {

    @Inject
    PrivilegeService privilegeService;

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    ActionService actionService;

    @Inject
    Vps4UserService userService;

    @Inject
    Vps4CpanelService cpanelService;

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
                    Vps4CpanelService cpServ = Mockito.mock(Vps4CpanelService.class);
                    bind(Vps4CpanelService.class).toInstance(cpServ);

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
    UUID centVmId;
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
        project = projService.createProject("TestProject", validUser.getId(), "vps4-testing-");
        createCpanelVm();
        createCentVm();
    }

    private void createCpanelVm() {
        virtualMachine = SqlTestData.insertTestVm(orionGuid, validUser.getId(), dataSource, "centos-7-cpanel-11");
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        vmId = virtualMachine.vmId;
    }

    private void createCentVm() {
        centVirtualMachine = SqlTestData.insertTestVm(centOrionGuid, validUser.getId(), dataSource);
        virtualMachineService.addHfsVmIdToVirtualMachine(centVirtualMachine.vmId, centHfsVmId);
        centVmId = centVirtualMachine.vmId;
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(centVirtualMachine.vmId, dataSource);
    }

    private CPanelResource getValidResource() {
        user = validUser;
        return injector.getInstance(CPanelResource.class);
    }

    private CPanelResource getInvalidResource() {
        user = invalidUser;
        return injector.getInstance(CPanelResource.class);
    }



    @Test
    public void testGetWHMSession(){
        getValidResource().getWHMSession(vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testGetWHMSessionInvalidUser(){
        getInvalidResource().getWHMSession(vmId);
    }

    @Test(expected=Vps4Exception.class)
    public void testGetWhmSessionInvalidImage(){
        getValidResource().getWHMSession(centVmId);
    }



    @Test
    public void testGetCPanelSession(){
        getValidResource().getCPanelSession(vmId, "testuser");
    }

    @Test(expected=AuthorizationException.class)
    public void testGetCPanelSessionInvalidUser(){
        getInvalidResource().getCPanelSession(vmId, "testuser");
    }

    @Test(expected=Vps4Exception.class)
    public void testGetCPanelSessionInvalidImage(){
        getValidResource().getCPanelSession(centVmId, "testuser");
    }


    @Test
    public void testListCpanelAccounts(){
        getValidResource().listCpanelAccounts(vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testListCpanelAccountsInvalidUser(){
        getInvalidResource().listCpanelAccounts(vmId);
    }

    @Test(expected=Vps4Exception.class)
    public void testListCpanelAccountsInvalidImage(){
        getValidResource().listCpanelAccounts(centVmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testVmIdDoesntExist(){
        // Authorization Exception is thrown because the valid user doesn't
        // have permissions on the vm with id randomUUID even though that UUID
        // doesn't even exist.
        getValidResource().listCpanelAccounts(UUID.randomUUID());
    }

}
