package com.godaddy.vps4.web.vm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.PaginatedResult;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import static org.junit.Assert.assertEquals;

public class GetVmActionsTest {

    ResultSubset<Action> actionSubset;

    UUID vmId = UUID.randomUUID();

    UriInfo uriInfo;

    @Before
    public void setupTest() {
        uriInfo = Mockito.mock(UriInfo.class);
        URI uri = null;
        try {
            uri = new URI("http://fakeUri/something/something/");
        } catch (URISyntaxException e) {
            // do nothing
        }
        Mockito.when(uriInfo.getAbsolutePath()).thenReturn(uri);
    }

    @Test
    public void testNullResults() {
        actionSubset = null;

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            public void configure() {
                PrivilegeService privilegeService = Mockito.mock(PrivilegeService.class);
                Mockito.when(
                        privilegeService.checkAnyPrivilegeToProjectId(Mockito.any(Vps4User.class), Mockito.anyLong()))
                        .thenReturn(true);
                bind(PrivilegeService.class).toInstance(privilegeService);

                ActionService actionService = Mockito.mock(ActionService.class);
                Mockito.when(actionService.getActions(Mockito.any(UUID.class), Mockito.anyLong(), Mockito.anyLong()))
                        .thenReturn(actionSubset);
                bind(ActionService.class).toInstance(actionService);

            }

            @Provides
            public Vps4User provideUser() {
                return new Vps4User(1, "123456666");
            }

        });

        VmActionResource vmActionResource = injector.getInstance(VmActionResource.class);
        PaginatedResult<Action> actionResults = vmActionResource.getActions(UUID.randomUUID(), 10, 10, uriInfo);
        System.out.println(actionResults.results.size());
        assertEquals(0, actionResults.pagination.total);
        assertEquals(new ArrayList<Action>(), actionResults.results);
    }

    @Test
    public void testNonNullResults() {
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(new Action(1, vmId, ActionType.STOP_VM, 1, "", "", "", ActionStatus.COMPLETE, null, "", null));
        actionSubset = new ResultSubset<Action>(actionList, 1);

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            public void configure() {
                PrivilegeService privilegeService = Mockito.mock(PrivilegeService.class);
                Mockito.when(
                        privilegeService.checkAnyPrivilegeToProjectId(Mockito.any(Vps4User.class), Mockito.anyLong()))
                        .thenReturn(true);
                bind(PrivilegeService.class).toInstance(privilegeService);

                ActionService actionService = Mockito.mock(ActionService.class);
                Mockito.when(actionService.getActions(Mockito.any(UUID.class), Mockito.anyLong(), Mockito.anyLong()))
                        .thenReturn(actionSubset);
                bind(ActionService.class).toInstance(actionService);
            }

            @Provides
            public Vps4User provideUser() {
                return new Vps4User(1, "123456666");
            }
        });

        VmActionResource vmActionResource = injector.getInstance(VmActionResource.class);
        PaginatedResult<Action> actionResults = vmActionResource.getActions(UUID.randomUUID(), 10, 10, uriInfo);
        System.out.println(actionResults.results.size());
        assertEquals(1, actionResults.pagination.total);
        assertEquals(actionList, actionResults.results);
    }

}
