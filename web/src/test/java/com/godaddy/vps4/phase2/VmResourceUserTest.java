package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.vm.VmNotFoundException;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmResource.ProvisionVmRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class VmResourceUserTest {

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    ActionService actionService;

    @Inject
    Vps4UserService userService;

    @Inject
    ProjectService projService;

    @Inject
    NetworkService networkService;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {
                
                @Override
                public void configure() {
                    // HFS services
                    Vm hfsVm = new Vm();
                    hfsVm.vmId = hfsVmId;
                    VmService vmService = Mockito.mock(VmService.class);
                    Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);

                    bind(CPanelService.class).toInstance(Mockito.mock(CPanelService.class));
                    bind(VmService.class).toInstance(vmService);

                    // Command Service
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class))).thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);
                }

                @Provides
                public Vps4User provideUser() {
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
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "cPanel", 10, 1, "validUserShopperId");
        project = projService.createProject("TestProject", validUser.getId(), 1, "vps4-test-");
        vmId = virtualMachineService.provisionVirtualMachine(orionGuid, "fakeVM", project.getProjectId(), 1, 1, 1);
        virtualMachineService.addHfsVmIdToVirtualMachine(vmId, hfsVmId);
        networkService.createIpAddress(1234, vmId, "127.0.0.1", IpAddressType.PRIMARY);
    }

    @After
    public void teardownTest(){
        DataSource dataSource = injector.getInstance(DataSource.class);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM ip_address where ip_address_id = ?", null, 1234);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        projService.deleteProject(project.getProjectId());
    }

    protected VmResource newValidVmResource() {
        user = validUser;
        return injector.getInstance(VmResource.class);
    }

    protected VmResource newInvalidVmResource() {
        user = invalidUser;
        return injector.getInstance(VmResource.class);
    }

    @Test
    public void testListActions(){
        long actionId = actionService.createAction(vmId, ActionType.CREATE_VM, "{}", validUser.getId());
        newValidVmResource().getAction(actionId);
        try{
            newInvalidVmResource().getAction(actionId);
            Assert.fail();
        }catch (Vps4Exception e){
            System.out.println(e);
        }
    }

    @Test
    public void testGetVm(){
        newValidVmResource().getVm(orionGuid);
        try{
            newInvalidVmResource().getVm(orionGuid);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }



    @Test
    public void testStartVm() throws VmNotFoundException{
        newValidVmResource().startVm(hfsVmId);
        try{
            newInvalidVmResource().startVm(hfsVmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testStopVm() throws VmNotFoundException{
        newValidVmResource().stopVm(hfsVmId);
        try{
            newInvalidVmResource().stopVm(hfsVmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testRestartVm() throws VmNotFoundException{
        newValidVmResource().restartVm(hfsVmId);
        try{
            newInvalidVmResource().restartVm(hfsVmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testDestroyVm() throws VmNotFoundException{
        newValidVmResource().destroyVm(hfsVmId);
        try{
            newInvalidVmResource().destroyVm(hfsVmId);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }

    @Test
    public void testProvisionVm() throws InterruptedException {
        UUID newGuid = UUID.randomUUID();
        virtualMachineService.createVirtualMachineRequest(newGuid, "linux", "cPanel", 10, 1, validUser.getShopperId());
        ProvisionVmRequest provisionRequest = new ProvisionVmRequest();
        provisionRequest.orionGuid = newGuid;
        provisionRequest.dataCenterId = 1;
        provisionRequest.image = "centos-7";
        provisionRequest.name = "Test Name";
        newValidVmResource().provisionVm(provisionRequest);
        try{
            newInvalidVmResource().provisionVm(provisionRequest);
            Assert.fail();
        }catch (Vps4Exception e) {
            //do nothing
        }
    }




}
