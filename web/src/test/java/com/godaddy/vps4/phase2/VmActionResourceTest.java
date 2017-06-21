package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmActionResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmActionResourceTest {

    private GDUser user;
    private UriInfo uri = mock(UriInfo.class);

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private VmActionResource getVmActionResource() {
        return injector.getInstance(VmActionResource.class);
    }

    @Before
    public void setupTest() throws URISyntaxException {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        when(uri.getAbsolutePath()).thenReturn(new URI("/vmid/actions"));
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VirtualMachine createTestVm(String shopperId) {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(shopperId);
        return SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
    }

    private Action createTestVmAction(UUID vmId, ActionType actionType) {
        UUID commandId = UUID.randomUUID();
        return SqlTestData.insertTestVmAction(commandId, vmId, actionType, dataSource);
    }

    @Test
    public void testShopperGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = action.commandId;

        action = getVmActionResource().getVmAction(vm.vmId, action.id);
        Assert.assertEquals(expectedGuid, action.commandId);
    }

    @Test(expected=NotFoundException.class)
    public void testUnauthorizedShopperGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createShopper("shopperX");
        action = getVmActionResource().getVmAction(vm.vmId, action.id);
    }

    @Test(expected=NotFoundException.class)
    public void testNoSuchAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        long actionId = 1234567L;

        getVmActionResource().getVmAction(vm.vmId, actionId);
    }

    @Test(expected=NotFoundException.class)
    public void testCannotGetVmActionOnDifferentVm() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createShopper("shopperX");
        VirtualMachine xvm = createTestVm(user.getShopperId());
        getVmActionResource().getVmAction(xvm.vmId, action.id);
    }

    @Test
    public void testEmployeeGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = action.commandId;

        user = GDUserMock.createEmployee();
        action = getVmActionResource().getVmAction(vm.vmId, action.id);
        Assert.assertEquals(expectedGuid, action.commandId);
    }

    @Test
    public void testE2SGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = action.commandId;

        user = GDUserMock.createEmployee2Shopper();
        action = getVmActionResource().getVmAction(vm.vmId, action.id);
        Assert.assertEquals(expectedGuid, action.commandId);
    }

    @Test
    public void testAdminGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = action.commandId;

        user = GDUserMock.createAdmin();
        action = getVmActionResource().getVmAction(vm.vmId, action.id);
        Assert.assertEquals(expectedGuid, action.commandId);
    }


    @Test
    public void testShopperListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action1 = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        Action action2 = createTestVmAction(vm.vmId, ActionType.STOP_VM);

        List<Action> actions = getVmActionResource().getActions(vm.vmId, 10, 0, null, uri).results;
        Assert.assertEquals(2, actions.size());

        List<UUID> commandIds = actions.stream().map(a -> a.commandId).collect(Collectors.toList());
        Assert.assertTrue(commandIds.contains(action1.commandId));
        Assert.assertTrue(commandIds.contains(action2.commandId));
    }

    @Test(expected=NotFoundException.class)
    public void testUnauthorizedShopperListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createShopper("shopperX");
        getVmActionResource().getActions(vm.vmId, 10, 0, null, uri);
    }

    @Test
    public void testNoActionsListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());

        List<Action> actions = getVmActionResource().getActions(vm.vmId, 10, 0, null, uri).results;
        Assert.assertTrue(actions.isEmpty());
    }

    @Test
    public void testAdminListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createAdmin();
        List<Action> actions = getVmActionResource().getActions(vm.vmId, 10, 0, null, uri).results;
        Assert.assertEquals(actions.get(0).commandId, action.commandId);
    }

    @Test
    public void testGetActionListWithStatusFilter() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        List<String> statusList = Arrays.asList("NEW");

        List<Action> actions = getVmActionResource().getActions(vm.vmId, 10, 0, statusList, uri).results;
        Assert.assertEquals(1, actions.size());

        statusList = Arrays.asList("COMPLETE");
        actions = getVmActionResource().getActions(vm.vmId, 10, 0, statusList, uri).results;
        Assert.assertTrue(actions.isEmpty());
    }
}

