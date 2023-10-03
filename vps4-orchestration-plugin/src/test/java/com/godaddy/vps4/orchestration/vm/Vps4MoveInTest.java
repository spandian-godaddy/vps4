package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddMonitoring;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4MoveInTest {
    @Mock private ActionService actionService;
    @Mock private CommandContext context;
    @Mock private CreditService creditService;

    @Captor private ArgumentCaptor<Function<CommandContext, Void>> voidCommandCaptor;
    @Captor private ArgumentCaptor<Map<ECommCreditService.ProductMetaField, String>> prodMetaCaptor;
    @Captor private ArgumentCaptor<Action> actionCaptor;
    @Captor private ArgumentCaptor<VmActionRequest> requestCaptor;

    @Mock private VirtualMachine vm;
    @Mock private Vps4MoveIn.Request request;

    private Vps4MoveIn command;

    @Before
    public void setUp() {
        command = new Vps4MoveIn(actionService, creditService);

        vm.orionGuid = UUID.randomUUID();
        vm.vmId = UUID.randomUUID();
        vm.dataCenter = new DataCenter();
        vm.dataCenter.dataCenterId = 1;

        request.actions = createMockActionList();
        request.virtualMachine = vm;

        when(context.getId()).thenReturn(UUID.randomUUID());
    }

    private List<Action> createMockActionList() {
        Random r = new Random();
        List<Action> list = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Action action = mock(Action.class);
            action.id = r.nextLong();
            list.add(action);
        }
        return list;
    }

    @Test
    public void updatesProdMeta() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("UpdateProdMeta"), voidCommandCaptor.capture(), eq(Void.class));
        voidCommandCaptor.getValue().apply(context);

        verify(creditService, times(1)).updateProductMeta(eq(vm.orionGuid), prodMetaCaptor.capture());
        Map<ECommCreditService.ProductMetaField, String> pm = prodMetaCaptor.getValue();
        assertEquals("" + vm.dataCenter.dataCenterId, pm.get(ECommCreditService.ProductMetaField.DATA_CENTER));
        assertEquals("" + vm.vmId, pm.get(ECommCreditService.ProductMetaField.PRODUCT_ID));
    }

    @Test
    public void insertsActionRecords() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("MoveInActions"), voidCommandCaptor.capture(), eq(Void.class));
        voidCommandCaptor.getValue().apply(context);

        verify(actionService, times(2)).insertAction(eq(vm.vmId), actionCaptor.capture());
        List<Action> result = actionCaptor.getAllValues();

        assertEquals(request.actions.get(0).id, result.get(0).id);
        assertEquals(request.actions.get(1).id, result.get(1).id);
    }

    @Test
    public void installsPanopta() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(Vps4AddMonitoring.class), requestCaptor.capture());
        VmActionRequest result = requestCaptor.getValue();
        assertEquals(vm, result.virtualMachine);
    }
}
