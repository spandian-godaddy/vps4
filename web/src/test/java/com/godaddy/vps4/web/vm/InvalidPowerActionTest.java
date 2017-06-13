package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Exception;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;

public class InvalidPowerActionTest {
    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    Vps4UserService userService;

    @Inject
    NetworkService networkService;

    @Inject
    ActionService actionService;

    CreditService creditService = Mockito.mock(CreditService.class);
    Vm hfsVm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CreditService.class).toInstance(creditService);

                    // HFS services
                    hfsVm = new Vm();
                    hfsVm.status = "ACTIVE";
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
        networkService.createIpAddress(1234, vmIds.get(0), "127.0.0.1", IpAddress.IpAddressType.PRIMARY);
    }

    @After
    public void teardownTest() {
        Sql.with(dataSource).exec("DELETE FROM ip_address where ip_address_id = ?", null, 1234);
        for (UUID vmId : vmIds) {
            SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        }
    }

    protected VmResource newValidVmResource() {
        user = validUser;
        return injector.getInstance(VmResource.class);
    }

    @Test(expected=Vps4Exception.class)
    public void testStartActiveVM() throws VmNotFoundException {
        hfsVm.status = "ACTIVE";
        newValidVmResource().startVm(vmIds.get(0));
    }

    @Test(expected=Vps4Exception.class)
    public void testStopInactiveVM() throws VmNotFoundException {
        hfsVm.status = "STOPPED";
        newValidVmResource().stopVm(vmIds.get(0));
    }

    @Test(expected=Vps4Exception.class)
    public void testRestartInactiveVM() throws VmNotFoundException {
        hfsVm.status = "STOPPED";
        newValidVmResource().restartVm(vmIds.get(0));
    }

    @Test
    public void testDoubleStartVmCompletedActionExists() throws VmNotFoundException {
        hfsVm.status = "STOPPED";
        long createActionId = actionService.createAction(vmIds.get(0), ActionType.START_VM, "{}", validUser.getId());
        actionService.completeAction(createActionId, "{}", "");
        VmResource vmResource = newValidVmResource();
        Action action = vmResource.startVm(vmIds.get(0));
        Assert.assertNotNull(action.commandId);
    }

    @Test
    public void testDoubleStartVM() throws VmNotFoundException {
        hfsVm.status = "STOPPED";
        VmResource vmResource = newValidVmResource();
        Action action = vmResource.startVm(vmIds.get(0));
        try {
            vmResource.startVm(vmIds.get(0));
            fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test
    public void testDoubleStopVM() throws VmNotFoundException {
        hfsVm.status = "ACTIVE";
        VmResource vmResource = newValidVmResource();
        Action action = vmResource.stopVm(vmIds.get(0));
        try {
            vmResource.stopVm(vmIds.get(0));
            fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test
    public void testDoubleRestartVM() throws VmNotFoundException {
        hfsVm.status = "ACTIVE";
        VmResource vmResource = newValidVmResource();
        Action action = vmResource.restartVm(vmIds.get(0));
        try {
            vmResource.restartVm(vmIds.get(0));
            fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test
    public void testStopWhileRestartingVM() throws VmNotFoundException {
        hfsVm.status = "ACTIVE";
        VmResource vmResource = newValidVmResource();
        Action action = vmResource.restartVm(vmIds.get(0));
        try {
            vmResource.stopVm(vmIds.get(0));
            fail("Exception not thrown");
        } catch (Vps4Exception e) {
            System.out.println(e.getId());
            Assert.assertNotNull(action.commandId);
        }
    }
}
