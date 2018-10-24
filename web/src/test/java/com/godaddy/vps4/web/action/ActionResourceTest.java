package com.godaddy.vps4.web.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionService.ActionListFilters;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Exception;

public class ActionResourceTest {


    private ActionService actionService = mock(ActionService.class);
    private ActionResource actionResource;
    private List<String> emptyList = new ArrayList<>();
    private ArgumentCaptor<ActionListFilters> argument = ArgumentCaptor.forClass(ActionListFilters.class);

    @Before
    public void setup() {
        actionResource = new ActionResource(actionService);
        when(actionService.getActionList(any())).thenReturn(null);
    }

    @Test
    public void testGetVmActionListEmpty() {
        List<Action> actions = actionResource.getVmActionList(null, emptyList, emptyList, null, null, 10, 0);
        Assert.assertEquals(0, actions.size());
    }

    @Test
    public void testGetVmActionListNonEmpty() {
        List<Action> testActions = Arrays.asList(mock(Action.class));
        when(actionService.getActionList(any())).thenReturn(new ResultSubset<Action>(testActions, 1));

        List<Action> actions = actionResource.getVmActionList(null, emptyList, emptyList, null, null, 10, 0);
        Assert.assertEquals(1, actions.size());
    }

    @Test
    public void testGetVmActionListNoFilters() {
        actionResource.getVmActionList(null, emptyList, emptyList, null, null, 10, 0);
        verify(actionService, times(1)).getActionList(argument.capture());

        ActionListFilters actionFilters = argument.getValue();
        assertNull(actionFilters.getVmId());
        assertEquals(emptyList, actionFilters.getStatusList());
        assertEquals(emptyList, actionFilters.getTypeList());
        assertNull(actionFilters.getStart());
        assertNull(actionFilters.getEnd());
        assertEquals(10, actionFilters.getLimit());
        assertEquals(0, actionFilters.getOffset());
    }

    @Test
    public void testGetVmActionListWithFilters() {
        UUID testVmId = UUID.randomUUID();
        List<String> testStatusList = Arrays.asList(ActionStatus.ERROR.toString(), ActionStatus.COMPLETE.toString());
        List<String> testTypeList = Arrays.asList(ActionType.CREATE_VM.toString(), ActionType.DESTROY_VM.toString());
        Instant testBeginDate = Instant.now().minus(Duration.ofHours(1));
        Instant testEndDate = Instant.now().plus(Duration.ofHours(1));
        long testLimit = 100;
        long testOffset = 10;

        actionResource.getVmActionList(testVmId, testStatusList, testTypeList,
                testBeginDate.toString(), testEndDate.toString(), testLimit, testOffset);
        verify(actionService, times(1)).getActionList(argument.capture());

        List<ActionStatus> expectedStatusList = testStatusList.stream().map(s -> ActionStatus.valueOf(s)).collect(Collectors.toList());
        List<ActionType> expectedTypeList = testTypeList.stream().map(t -> ActionType.valueOf(t)).collect(Collectors.toList());

        ActionListFilters actionFilters = argument.getValue();
        assertEquals(testVmId, actionFilters.getVmId());
        assertEquals(expectedStatusList, actionFilters.getStatusList());
        assertEquals(expectedTypeList, actionFilters.getTypeList());
        assertEquals(testBeginDate, actionFilters.getStart());
        assertEquals(testEndDate, actionFilters.getEnd());
        assertEquals(testLimit, actionFilters.getLimit());
        assertEquals(testOffset, actionFilters.getOffset());
    }

    @Test(expected = Vps4Exception.class)
    public void testGetVmActionListInvalidDate() {
        actionResource.getVmActionList(null, emptyList, emptyList, "not-a-date", null, 10, 0);
    }
}
