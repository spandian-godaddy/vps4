package com.godaddy.vps4.web.vm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;

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
        uriInfo = mock(UriInfo.class);
        URI uri = null;
        try {
            uri = new URI("http://fakeUri/something/something/");
        } catch (URISyntaxException e) {
            // do nothing
        }
        when(uriInfo.getAbsolutePath()).thenReturn(uri);
    }

    @Test
    public void testNullResults() {
        actionSubset = null;
        ActionService actionService = mock(ActionService.class);
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            public void configure() {
                PrivilegeService privilegeService = mock(PrivilegeService.class);
                when(
                        privilegeService.checkAnyPrivilegeToProjectId(any(Vps4User.class), anyLong()))
                        .thenReturn(true);
                bind(PrivilegeService.class).toInstance(privilegeService);
                
                when(actionService.getActions(any(UUID.class), anyLong(), anyLong()))
                        .thenReturn(actionSubset);
                bind(ActionService.class).toInstance(actionService);

            }

            @Provides
            public Vps4User provideUser() {
                return new Vps4User(1, "123456666");
            }

        });

        VmActionResource vmActionResource = injector.getInstance(VmActionResource.class);
        PaginatedResult<Action> actionResults = vmActionResource.getActions(vmId, 10, 10, null, uriInfo);
        verify(actionService, times(1)).getActions(vmId, 10, 10);
        assertEquals(0, actionResults.pagination.total);
        assertEquals(new ArrayList<Action>(), actionResults.results);
    }

    @Test
    public void testNonNullResults() {
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(new Action(1, vmId, ActionType.STOP_VM, 1, "", "", "", ActionStatus.COMPLETE, null, "", null));
        actionSubset = new ResultSubset<Action>(actionList, 1);
        ActionService actionService = mock(ActionService.class);
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            public void configure() {
                PrivilegeService privilegeService = mock(PrivilegeService.class);
                when(privilegeService.checkAnyPrivilegeToProjectId(any(Vps4User.class), anyLong()))
                        .thenReturn(true);
                bind(PrivilegeService.class).toInstance(privilegeService);
                
                when(actionService.getActions(any(UUID.class), anyLong(), anyLong()))
                        .thenReturn(actionSubset);
                bind(ActionService.class).toInstance(actionService);
            }

            @Provides
            public Vps4User provideUser() {
                return new Vps4User(1, "123456666");
            }
        });
        VmActionResource vmActionResource = injector.getInstance(VmActionResource.class);
        PaginatedResult<Action> actionResults = vmActionResource.getActions(vmId, 10, 10, null, uriInfo);
        verify(actionService, times(1)).getActions(vmId, 10, 10);
        assertEquals(1, actionResults.pagination.total);
        assertEquals(actionList, actionResults.results);
    }
    
    @Test
    public void testFilteredResults() {
        // uses a different getActions, since there is now a non-empty list being passed to vmActionResource.getActions
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(new Action(1, vmId, ActionType.STOP_VM, 1, "", "", "", ActionStatus.COMPLETE, null, "", null));
        actionSubset = new ResultSubset<Action>(actionList, 1);
        ActionService actionService = mock(ActionService.class);
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            public void configure() {
                PrivilegeService privilegeService = mock(PrivilegeService.class);
                when(
                        privilegeService.checkAnyPrivilegeToProjectId(any(Vps4User.class), anyLong()))
                        .thenReturn(true);
                bind(PrivilegeService.class).toInstance(privilegeService);

                when(actionService.getActions(any(UUID.class), anyLong(), anyLong(), anyList()))
                        .thenReturn(actionSubset);
                bind(ActionService.class).toInstance(actionService);
            }

            @Provides
            public Vps4User provideUser() {
                return new Vps4User(1, "123456666");
            }
        });

        VmActionResource vmActionResource = injector.getInstance(VmActionResource.class);
        List<String> statusList = Arrays.asList(new String[]{"NEW", "IN PROGRESS"});
        PaginatedResult<Action> actionResults = vmActionResource.getActions(vmId, 10, 10, statusList, uriInfo);
        verify(actionService, times(1)).getActions(vmId, 10, 10, statusList);
        assertEquals(1, actionResults.pagination.total);
        assertEquals(actionList, actionResults.results);
    }

}
