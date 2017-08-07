package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmPatchResource.VmPatch;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmPatchResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject VirtualMachineService virtualMachineService;
    ActionService actionService = mock(ActionService.class);

    private GDUser user;
    private String initialName;
    private VirtualMachine virtualMachine;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    // Action service
                    Action coreVmAction = new Action(123L, UUID.randomUUID(), ActionType.UPDATE_SERVER,
                            123L, "", "", "", ActionStatus.COMPLETE,
                            Instant.now(), "", UUID.randomUUID());
                    Mockito.when(actionService.getAction(Mockito.anyLong()))
                            .thenReturn(coreVmAction);
                    bind(ActionService.class).toInstance(actionService);
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                    bind(ImageService.class).toInstance(Mockito.mock(ImageService.class));
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();

        virtualMachine = createTestVm();
        initialName = virtualMachine.name;
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmPatchResource getVmPatchResource() {
        return injector.getInstance(VmPatchResource.class);
    }

    private void testValidServerName(String newName){
        VirtualMachine vm = updateVmName(newName);
        assertEquals(newName, vm.name);
        verify(actionService, times(1)).completeAction(anyLong(), eq("{}"), eq("Name = "+newName));
    }

    private VirtualMachine updateVmName(String newName) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(initialName, vm.name);
        VmPatch vmPatch = new VmPatch();
        vmPatch.name = newName;
        getVmPatchResource().updateVm(vm.vmId, vmPatch);
        vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        return vm;
    }

    @Test
    public void testShopperUpdateServerName(){
        testValidServerName("NewVmName");
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthShopperUpdateName() {
        user = GDUserMock.createShopper("shopperX");
        testValidServerName("NewVmName");
    }

    @Test
    public void testAdminUpdateServerName(){
        user = GDUserMock.createAdmin();
        testValidServerName("NewVmName");
    }

    @Test
    public void testPunctuationAllowed(){
        testValidServerName("PunctuationOkay!@#$%^&*()-=+\"'");
    }

    @Test
    public void testSpacesOkay(){
        testValidServerName("This VM Name Has Spaces");
    }

    @Test
    public void testNonAlphabetical(){
        testValidServerName("º∂å∑¬˚∆´");
    }

    @Test
    public void testEmptyName(){
        // When an empty string is passed there is no change.
        VirtualMachine vm = updateVmName(new String(""));
        assertEquals(initialName, vm.name);
    }

    @Test
    public void testNoName() {
        // When name is not passed there is no change.
        VmPatch vmPatch = new VmPatch();
        getVmPatchResource().updateVm(virtualMachine.vmId, vmPatch);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(initialName, vm.name);
    }
}
