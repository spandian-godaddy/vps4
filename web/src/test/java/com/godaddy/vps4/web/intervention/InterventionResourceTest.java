package com.godaddy.vps4.web.intervention;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

@RunWith(MockitoJUnitRunner.class)
public class InterventionResourceTest {
    @Captor private ArgumentCaptor<ActionListFilters> alfCaptor;

    @Mock private ActionService actionService;
    @Mock private VirtualMachineService virtualMachineService;

    private final InterventionResource.Request request = new InterventionResource.Request();
    private final List<Action> interventions = new ArrayList<>();
    private final String reason = "Testing intervention endpoint";
    private final String jsonReason = String.format("{\"reason\":\"%s\"}", reason);
    private final UUID vmId = UUID.randomUUID();

    @Mock private Action action;
    @Mock private Action conflictingAction;
    @Mock private VirtualMachine vm;

    private GDUser user;
    private InterventionResource resource;

    @Before
    public void setUpTests() {
        setUpObjects();
        setUpInteractions();
    }

    private void setUpObjects() {
        action.created = Instant.now();
        action.id = new Random().nextLong();
        action.request = jsonReason;
        action.resourceId = vmId;
        action.type = ActionType.INTERVENTION;
        conflictingAction.id = new Random().nextLong();
        conflictingAction.type = ActionType.INTERVENTION;
        vm.validUntil = Instant.MAX;
        request.reason = reason;
        interventions.add(action);
        user = GDUserMock.createAdmin();
        resource = new InterventionResource(user, actionService, virtualMachineService);
    }

    private void setUpInteractions() {
        when(actionService.createAction(eq(vmId), eq(ActionType.INTERVENTION), eq(jsonReason), anyString()))
                .thenReturn(action.id);
        when(actionService.getAction(action.id)).thenReturn(action);
        when(actionService.getAction(conflictingAction.id)).thenReturn(conflictingAction);
        when(actionService.getActionList(any(ActionListFilters.class)))
                .thenReturn(new ResultSubset<>(interventions, interventions.size()));
        doNothing().when(actionService).completeAction(anyLong(), eq(null), eq(null));
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
    }

    @Test
    public void requiresAdminRole() {
        assertTrue(InterventionResource.class.isAnnotationPresent(RequiresRole.class));
        GDUser.Role[] roles = InterventionResource.class.getAnnotation(RequiresRole.class).roles();
        assertEquals(2, roles.length);
        assertTrue(Arrays.stream(roles).anyMatch(r -> r == GDUser.Role.ADMIN));
    }

    @Test
    public void startInterventionCreatesAction() throws JsonProcessingException {
        VmAction result = resource.startIntervention(vmId, request);
        verify(actionService, times(1)).createAction(eq(vmId), eq(ActionType.INTERVENTION), eq(jsonReason), anyString());
        verify(actionService, times(1)).markActionInProgress(action.id);
        assertEquals(action.id, result.id);
    }

    @Test
    public void startInterventionThrows404() throws JsonProcessingException {
        try {
            resource.startIntervention(UUID.randomUUID(), request);
            fail();
        } catch (NotFoundException ignored) {}
    }

    @Test
    public void startInterventionConflictingAction() throws JsonProcessingException {
        when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictingAction));
        try {
            resource.startIntervention(vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void endIntervention() {
        VmAction result = resource.endIntervention(vmId);
        verify(actionService, times(1)).getActionList(any());
        verify(actionService, times(1)).completeAction(action.id, null, null);
        assertEquals(result.id, action.id);
    }

    @Test
    public void endInterventionActionFilters() {
        resource.endIntervention(vmId);
        verify(actionService, times(1)).getActionList(alfCaptor.capture());
        ActionListFilters value = alfCaptor.getValue();
        assertEquals(vmId, value.getResourceId());
        assertTrue(value.getTypeList().contains(ActionType.INTERVENTION));
        assertTrue(value.getStatusList().contains(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void endInterventionThrows404() {
        try {
            resource.endIntervention(UUID.randomUUID());
            fail();
        } catch (NotFoundException ignored) {}
    }

    @Test
    public void endInterventionConflictingAction() throws JsonProcessingException {
        when(actionService.getActionList(any())).thenReturn(null);
        try {
            resource.endIntervention(vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_STATE", e.getId());
        }
    }
}
