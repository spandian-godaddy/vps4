package com.godaddy.vps4.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.ArgumentMatcher;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class Vps4MessageHandlerTest {

    private VirtualMachineService vmServiceMock = mock(VirtualMachineService.class);
    private SnapshotService snapshotServiceMock = mock(SnapshotService.class);
    private CreditService creditServiceMock = mock(CreditService.class);
    private ActionService vmActionServiceMock = mock(ActionService.class);
    private ActionService snapshotActionServiceMock = mock(ActionService.class);
    private CommandService commandServiceMock = mock(CommandService.class);
    private DataCenterService dcService = mock(DataCenterService.class);
    private Config configMock = mock(Config.class);
    private Vps4MessagingService messagingServiceMock = mock(Vps4MessagingService.class);

    private UUID orionGuid;
    private VirtualMachine vm;

    @Before
    public void setupTest() {
        when(dcService.getDataCenter(5)).thenReturn(new DataCenter(5,"testDataCenter"));
        when(configMock.get("nodeping.accountid")).thenReturn("1");

        orionGuid = UUID.randomUUID();

        vm = new VirtualMachine(UUID.randomUUID(), 123L, orionGuid,
                321L, null, "TestVm", null, null, null, null, null, 0, UUID.randomUUID());
        when(vmServiceMock.getVirtualMachine(vm.vmId)).thenReturn(vm);

        CommandState command = new CommandState();
        command.commandId = UUID.randomUUID();
        when(commandServiceMock.executeCommand(anyObject())).thenReturn(command);

    }

    private void mockVmCredit(AccountStatus accountStatus, UUID productId) {
        DataCenter dc = dcService.getDataCenter(5);
        VirtualMachineCredit vmCredit = new VirtualMachineCredit(orionGuid, 10, 0, 1, "linux", "myh", null, "TestShopper", accountStatus, dc, productId, false);
        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
    }
    
    private void mockFullManagedVmCredit(AccountStatus accountStatus, UUID productId) {
        DataCenter dc = dcService.getDataCenter(5);
        VirtualMachineCredit vmCredit = new VirtualMachineCredit(orionGuid, 10, 2, 1, "linux", "cpanel", null, "TestShopper", accountStatus, dc, productId, false);
        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
    }
    
    @SuppressWarnings("unchecked")
    private String createTestKafkaMessage(String type) {
        /*
         * Sample:
         *  {"id": "a82c9629-3e19-4b3a-a870-edc0059eebe5",
         *   "notification": {
         *      "type": ["added"],
         *      "account_guid": "e36b4412-ec52-420f-86fd-cf5332cf0c88"}
         *  }
         */
        JSONObject notification = new JSONObject();
        notification.put("type", type);
        notification.put("account_guid", orionGuid.toString());

        JSONObject kafkaMsg = new JSONObject();
        kafkaMsg.put("id", UUID.randomUUID().toString());
        kafkaMsg.put("notification", notification);

        return kafkaMsg.toJSONString();
    }

    private void callHandleMessage(String message) throws MessageHandlerException {
        MessageHandler handler = new Vps4MessageHandler(vmServiceMock,
                snapshotServiceMock,
                creditServiceMock,
                vmActionServiceMock,
                snapshotActionServiceMock,
                commandServiceMock,
                messagingServiceMock,
                configMock);
        handler.handleMessage(message);
    }

    @Test(expected = MessageHandlerException.class)
    public void testHandleMessageBadJson() throws MessageHandlerException {
        callHandleMessage("bad json");
    }

    @Test(expected = MessageHandlerException.class)
    public void testHandleMessageBadValues() throws MessageHandlerException {
        callHandleMessage("{\"id\":\"not a guid\","
                + "\"notification\":{\"type\":[\"added\"],"
                + "\"account_guid\":\"e36b4412-ec52-420f-86fd-cf5332cf0c88\"}}");
    }

    @Test
    public void testNoVmCreditCausesNoChange() throws MessageHandlerException {
        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(null);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, never()).executeCommand(anyObject());
    }

    @Test
    public void testHandleMessageAddedCausesNoChange() throws MessageHandlerException, MissingShopperIdException, IOException {
        mockVmCredit(AccountStatus.ACTIVE, null);
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, times(0)).executeCommand(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(Mockito.anyString(), Mockito.anyString());
    }
    
    @Test
    public void testFullyManagedCredit() throws MessageHandlerException, MissingShopperIdException, IOException {
        mockFullManagedVmCredit(AccountStatus.ACTIVE, vm.vmId);
        when(messagingServiceMock.sendFullyManagedEmail("TestShopper", "cpanel")).thenReturn("messageId");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");
        Mockito.doNothing().when(creditServiceMock).updateProductMeta(orionGuid, ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");
        Mockito.doNothing().when(vmServiceMock).updateVirtualMachine(Mockito.eq(vm.vmId), Mockito.anyMap());
        
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, times(1)).sendFullyManagedEmail("TestShopper", "cpanel");
        verify(creditServiceMock, times(1)).updateProductMeta(orionGuid, ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");
        ArgumentMatcher<Map<String, Object>> matchesFullyManaged = new ArgumentMatcher<Map<String, Object>>() {
            @Override
            public boolean matches(Object argument) {
                @SuppressWarnings("unchecked")
                int newManagedLevel = (int)(((Map<String, Object>)argument).get("managed_level"));
                return newManagedLevel == 2;
            }
        };
        verify(vmServiceMock, times(1)).updateVirtualMachine(Mockito.eq(vm.vmId), Mockito.argThat(matchesFullyManaged));
    }

    @Test
    public void testDontProcessFullyManagedEmails() throws MessageHandlerException, MissingShopperIdException, IOException {
        mockFullManagedVmCredit(AccountStatus.ACTIVE, null);
        when(messagingServiceMock.sendFullyManagedEmail("TestShopper", "cpanel")).thenReturn("messageId");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("false");
        
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(Mockito.anyString(), Mockito.anyString());
    }
    
    @Test
    public void testFullyManagedEmailAlreadySent() throws MessageHandlerException, MissingShopperIdException, IOException {
        DataCenter dc = dcService.getDataCenter(5);
        VirtualMachineCredit vmCredit = new VirtualMachineCredit(orionGuid, 10, 2, 1, "linux", "cpanel", null, "TestShopper", AccountStatus.ACTIVE, dc, null, true);

        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
        when(messagingServiceMock.sendFullyManagedEmail("TestShopper", "cpanel")).thenReturn("messageId");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");
        
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testHandleMessageSuspendedNoProductId() throws MessageHandlerException {
        mockVmCredit(AccountStatus.SUSPENDED, null);
        callHandleMessage(createTestKafkaMessage("suspended"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, never()).executeCommand(anyObject());
    }

    @Test
    public void testHandleMessageSuspended() throws MessageHandlerException {
        mockVmCredit(AccountStatus.SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("suspended"));

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4StopVm", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testHandleMessageAbuseSuspended() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ABUSE_SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("suspended"));

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4StopVm", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testHandleMessageReinstatedCausesNoChange() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("reinstated"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, never()).executeCommand(anyObject());
    }

    @Test
    public void testHandleMessageRemovedNoProductId() throws MessageHandlerException {
        mockVmCredit(AccountStatus.REMOVED, null);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, never()).executeCommand(anyObject());
    }

    @Test
    public void testHandleMessageRemovedNoSnapshots() throws MessageHandlerException {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);

        when(snapshotServiceMock.getSnapshotsByOrionGuid(orionGuid)).thenReturn(Collections.emptyList());
        callHandleMessage(createTestKafkaMessage("removed"));

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4DestroyVm", argument.getValue().commands.get(0).command);
        verify(vmServiceMock, times(1)).destroyVirtualMachine(anyLong());
    }

    @Test
    public void testHandleMessageRemovedWithSnapshots() throws MessageHandlerException {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);

        Snapshot snapshot = mock(Snapshot.class);
        when(snapshotServiceMock.getSnapshotsByOrionGuid(orionGuid)).thenReturn(Collections.singletonList(snapshot));
        callHandleMessage(createTestKafkaMessage("removed"));

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(2)).executeCommand(argument.capture());
        assertEquals("Vps4DestroySnapshot", argument.getValue().commands.get(0).command);
        verify(vmServiceMock, times(1)).destroyVirtualMachine(anyLong());
    }

    @Test
    public void testHandleMessageRemovedWithDestroyedSnapshots() throws MessageHandlerException {
        mockVmCredit(AccountStatus.REMOVED, null);

        Snapshot snapshot = new Snapshot(null, 0, null, null,
                SnapshotStatus.DESTROYED, null, null, null, 0, SnapshotType.ON_DEMAND);
        when(snapshotServiceMock.getSnapshotsByOrionGuid(orionGuid)).thenReturn(Collections.singletonList(snapshot));
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, never()).executeCommand(anyObject());
        verify(vmServiceMock, times(0)).destroyVirtualMachine(anyLong());
    }
}
