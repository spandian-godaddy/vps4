package com.godaddy.vps4.orchestration.hfs.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.hfs.network.WaitForAddressAction;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.AddressAction.Status;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class WaitForAddressActionTest {

    NetworkServiceV2 networkService = mock(NetworkServiceV2.class);
    CommandContext context = mock(CommandContext.class);

    WaitForAddressAction command = new WaitForAddressAction(networkService);
    long hfsAddressId = 42;
    long hfsAddressActionId = 23;
    long magicSleepTime = 2000;

    private AddressAction createMockAddressAction(Status status) {
        AddressAction mockAction = mock(AddressAction.class);
        mockAction.addressId = hfsAddressId;
        mockAction.addressActionId = hfsAddressActionId;
        mockAction.status = status;
        return mockAction;
    }

    @Test
    public void testExecuteWaitForComplete() {
        AddressAction newHfsAction = createMockAddressAction(Status.NEW);
        AddressAction inProgressHfsAction = createMockAddressAction(Status.IN_PROGRESS);
        AddressAction completeHfsAction = createMockAddressAction(Status.COMPLETE);

        // simulates address action progressing new->in_progress->complete
        when(networkService.getAddressAction(hfsAddressId, hfsAddressActionId))
            .thenReturn(inProgressHfsAction)
            .thenReturn(completeHfsAction);

        AddressAction result = command.execute(context, newHfsAction);
        verify(networkService, times(2)).getAddressAction(hfsAddressId, hfsAddressActionId);
        verify(context, times(2)).sleep(magicSleepTime);
        assertEquals(completeHfsAction, result);
    }

    @Test(expected=RuntimeException.class)
    public void testExecuteActionFails() {
        AddressAction newHfsAction = createMockAddressAction(Status.NEW);
        AddressAction failedHfsAction = createMockAddressAction(Status.FAILED);

        when(networkService.getAddressAction(hfsAddressId, hfsAddressActionId)).thenReturn(failedHfsAction);

        command.execute(context, newHfsAction);
    }

}
