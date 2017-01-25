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
import com.godaddy.vps4.vm.VirtualMachineService;
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
    long hfsVmId = 98765;
    Project project;
    UUID vmId;
    
    
    
    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachineService.createVirtualMachineCredit(orionGuid, "linux", "cPanel", 10, 1, "validUserShopperId");
        project = projService.createProject("TestProject", validUser.getId(), 1, "vps4-test-");
        vmId = virtualMachineService.provisionVirtualMachine(orionGuid, "fakeVM", project.getProjectId(), 1, 1, 1);
        virtualMachineService.addHfsVmIdToVirtualMachine(vmId, hfsVmId);
    }
    
    @After
    public void teardownTest(){
        DataSource dataSource = injector.getInstance(DataSource.class);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, orionGuid);
        projService.deleteProject(project.getProjectId());
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
        getValidResource().getWHMSession(hfsVmId);
        try{
            getInvalidResource().getWHMSession(hfsVmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testGetCPanelSession(){
        getValidResource().getCPanelSession(hfsVmId, "testuser");
        try{
            getInvalidResource().getCPanelSession(hfsVmId, "testuser");
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
    
    @Test
    public void testListCpanelAccounts(){
        getValidResource().listCpanelAccounts(hfsVmId);
        try{
            getInvalidResource().listCpanelAccounts(hfsVmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
}
