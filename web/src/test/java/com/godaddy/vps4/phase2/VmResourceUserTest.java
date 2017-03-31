package com.godaddy.vps4.phase2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.ActionResource;
import com.godaddy.vps4.web.vm.VmActionResource;
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

    private Injector injector = Guice.createInjector(new DatabaseModule(), new SecurityModule(), new VmModule(),
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
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                            .thenReturn(commandState);
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

    List<UUID> orionGuids = new ArrayList<UUID>();
    long hfsVmId = 98765;
    List<UUID> vmIds = new ArrayList<UUID>();
    DataSource dataSource = injector.getInstance(DataSource.class);

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        orionGuids.add(UUID.randomUUID());
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        vmIds.add(SqlTestData.insertTestVm(orionGuids.get(0), validUser.getId(), dataSource).vmId);
        virtualMachineService.addHfsVmIdToVirtualMachine(vmIds.get(0), hfsVmId);
        networkService.createIpAddress(1234, vmIds.get(0), "127.0.0.1", IpAddressType.PRIMARY);
    }

    @After
    public void teardownTest() {
        Sql.with(dataSource).exec("DELETE FROM ip_address where ip_address_id = ?", null, 1234);
        for (UUID vmId : vmIds) {
            SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        }
        for (UUID orionGuid : orionGuids) {
            Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, orionGuid);
        }
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
    public void testListActions() {
        long actionId = actionService.createAction(vmIds.get(0), ActionType.CREATE_VM, "{}", validUser.getId());
        user = validUser;
        ActionResource validActionResource = injector.getInstance(ActionResource.class);
        validActionResource.getAction(actionId);
    }

    @Test(expected = AuthorizationException.class)
    public void testListActionsInvalidUser() {
        long actionId = actionService.createAction(vmIds.get(0), ActionType.CREATE_VM, "{}", validUser.getId());
        user = validUser;
        ActionResource validActionResource = injector.getInstance(ActionResource.class);
        user = invalidUser;
        ActionResource invalidActionResource = injector.getInstance(ActionResource.class);
        invalidActionResource.getAction(actionId);
    }

    @Test
    public void testGetVmAction() {
        UUID vmId = vmIds.get(0);
        long actionId = actionService.createAction(vmId, ActionType.CREATE_VM, "{}", validUser.getId());
        user = validUser;
        VmActionResource validActionResource = injector.getInstance(VmActionResource.class);
        validActionResource.getVmAction(vmId, actionId);

    }

    @Test(expected = AuthorizationException.class)
    public void testGetVmActionInvalidUser() {
        UUID vmId = vmIds.get(0);
        long actionId = actionService.createAction(vmId, ActionType.CREATE_VM, "{}", validUser.getId());
        user = invalidUser;
        VmActionResource invalidActionResource = injector.getInstance(VmActionResource.class);
        invalidActionResource.getVmAction(vmId, actionId);
    }

    @Test
    public void testGetVm() {
        newValidVmResource().getVm(vmIds.get(0));
    }

    @Test(expected=NotFoundException.class)
    public void testGetVmNotFound() {
        newInvalidVmResource().getVm(vmIds.get(0));
    }

    @Test
    public void testStartVm() throws VmNotFoundException{
        Action action = newValidVmResource().startVm(vmIds.get(0));
        Assert.assertNotNull(action.commandId);
    }

    @Test(expected=NotFoundException.class)
    public void testStartVmInvalid() throws VmNotFoundException {
        newInvalidVmResource().startVm(vmIds.get(0));
    }

    @Test
    public void testStopVm() throws VmNotFoundException {
        Action action = newValidVmResource().stopVm(vmIds.get(0));
        Assert.assertNotNull(action.commandId);
    }

    @Test(expected=NotFoundException.class)
    public void testRestartVmInvalid() throws VmNotFoundException {
        newInvalidVmResource().restartVm(vmIds.get(0));
    }

    @Test
    public void testDestroyVm() throws VmNotFoundException {
        Action action = newValidVmResource().destroyVm(vmIds.get(0));
        Assert.assertNotNull(action.commandId);
    }

    @Test(expected = AuthorizationException.class)
    public void testDestroyVmInvalid() throws VmNotFoundException {
        newInvalidVmResource().destroyVm(vmIds.get(0));
    }

    private ProvisionVmRequest createProvisionRequest(String controlPanel) {
        UUID newGuid = UUID.randomUUID();
        orionGuids.add(newGuid);
        virtualMachineService.createVirtualMachineCredit(newGuid, "linux", controlPanel, 10, 1, validUser.getShopperId());
        ProvisionVmRequest provisionRequest = new ProvisionVmRequest();
        provisionRequest.orionGuid = newGuid;
        provisionRequest.dataCenterId = 1;
        provisionRequest.image = "centos-7";
        provisionRequest.name = "Test Name";
        return provisionRequest;
    }

    @Test
    public void testProvisionVm() throws InterruptedException {
        ProvisionVmRequest provisionRequest = createProvisionRequest("none");
        Action action = newValidVmResource().provisionVm(provisionRequest);
        vmIds.add(action.virtualMachineId);
        Assert.assertNotNull(action.commandId);

    }

    @Test(expected = AuthorizationException.class)
    public void testProvisionVmInvalidResource() throws InterruptedException {
        ProvisionVmRequest provisionRequest = createProvisionRequest("none");
        newInvalidVmResource().provisionVm(provisionRequest);
    }

    @Test
    public void testProvisionVmInvalidCredit() throws InterruptedException {
        // Verify that if a provision request image does not match the credits
        // os and control panel
        // an exception is thrown.
        ProvisionVmRequest provisionRequest = createProvisionRequest("cpanel");

        try {
            vmIds.add(newValidVmResource().provisionVm(provisionRequest).virtualMachineId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
            // do nothing
        }
    }

}
