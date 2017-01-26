package com.godaddy.vps4.security.api.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.cpanel.CPanelResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

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

    Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
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

    UUID orionGuid;
    Project project;
    UUID vmId;
    long hfsVmId = 98765;
    VirtualMachine virtualMachine;
    
    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachineService.createVirtualMachineCredit(orionGuid, "linux", "cPanel", 10, 1, "validUserShopperId");
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(validUser.getId(), 1, "vps4-testing-", orionGuid,
                "fakeVM", 10, 1, "centos-7");
        virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
    }

    @After
    public void teardownTest(){
        DataSource dataSource = injector.getInstance(DataSource.class);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, virtualMachine.vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, virtualMachine.vmId);
        Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, orionGuid);
        projService.deleteProject(virtualMachine.projectId);
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
        try{
            getInvalidResource().getWHMSession(vmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testGetCPanelSession(){
        getValidResource().getCPanelSession(vmId, "testuser");
        try{
            getInvalidResource().getCPanelSession(vmId, "testuser");
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testListCpanelAccounts(){
        getValidResource().listCpanelAccounts(vmId);
        try{
            getInvalidResource().listCpanelAccounts(vmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
}
