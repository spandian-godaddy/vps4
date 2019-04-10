package com.godaddy.vps4.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.client.VmZombieService;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class Vps4AccountMessageHandlerTest {

    private VirtualMachineService vmServiceMock = mock(VirtualMachineService.class);
    private CreditService creditServiceMock = mock(CreditService.class);
    private ActionService vmActionServiceMock = mock(ActionService.class);
    private CommandService commandServiceMock = mock(CommandService.class);
    private DataCenterService dcService = mock(DataCenterService.class);
    private VmZombieService vmZombieService = mock(VmZombieService.class);
    private Config configMock = mock(Config.class);
    private Vps4MessagingService messagingServiceMock = mock(Vps4MessagingService.class);

    private UUID orionGuid;
    private VirtualMachine vm;

    @Before
    public void setupTest() {
        when(dcService.getDataCenter(5)).thenReturn(new DataCenter(5,"testDataCenter"));
        when(configMock.get("monitoring.nodeping.account.id")).thenReturn("0");

        orionGuid = UUID.randomUUID();

        ServerSpec vmSpec = new ServerSpec();
        vmSpec.tier = 10;

        vm = new VirtualMachine(UUID.randomUUID(), 123L, orionGuid,
                321L, vmSpec, "TestVm", null, null, null, null, null, null, 0, UUID.randomUUID());
        when(vmServiceMock.getVirtualMachine(vm.vmId)).thenReturn(vm);

        CommandState command = new CommandState();
        command.commandId = UUID.randomUUID();
        when(commandServiceMock.executeCommand(anyObject())).thenReturn(command);
    }

    private void mockVmCredit(AccountStatus accountStatus, UUID productId) {
        mockVmCredit(accountStatus, productId, 10, 0, "myh");
    }

    private void mockFullManagedVmCredit(AccountStatus accountStatus, UUID productId) {
        mockVmCredit(accountStatus, productId, 10, 2, "cpanel");
    }

    private void mockVmCredit(AccountStatus accountStatus, UUID productId, int tier, int managedLevel, String controlPanel) {
        DataCenter dc = dcService.getDataCenter(5);
        VirtualMachineCredit vmCredit = new VirtualMachineCredit(orionGuid, tier, managedLevel, 0, "linux", controlPanel, null, "TestShopper", accountStatus, dc, productId, false, "1", false, 0);
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

    @SuppressWarnings("unchecked")
    private void callHandleMessage(String message) throws MessageHandlerException {
        ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(message);
        MessageHandler handler = new Vps4AccountMessageHandler(vmServiceMock,
                creditServiceMock,
                vmActionServiceMock,
                commandServiceMock,
                messagingServiceMock,
                vmZombieService,
                configMock);
        handler.handleMessage(record);
    }

    @Test(expected = MessageHandlerException.class)
    public void testHandleMessageBadJson() throws MessageHandlerException {
        callHandleMessage("bad json");
    }

    @Test(expected = IllegalArgumentException.class)
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

        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, times(1)).sendFullyManagedEmail("TestShopper", "cpanel");
        verify(creditServiceMock, times(1)).updateProductMeta(orionGuid, ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4PlanChange", argument.getValue().commands.get(0).command);
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
        VirtualMachineCredit vmCredit = new VirtualMachineCredit(orionGuid, 10, 2, 1, "linux", "cpanel", null, "TestShopper", AccountStatus.ACTIVE, dc, null, true, "1", false, 0);

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
    public void testHandleMessageAbuseSuspendedVmNotFound() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ABUSE_SUSPENDED, UUID.randomUUID());
        callHandleMessage(createTestKafkaMessage("suspended"));

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(0)).executeCommand(argument.capture());
    }

    @Test
    public void testHandleMessageCausesPlanChange() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("reinstated"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, times(1)).executeCommand(anyObject());
    }

    @Test
    public void accountRemovalZombiesAssociatedVm() throws MessageHandlerException {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(vmZombieService, times(1)).zombieVm(vm.vmId);
    }

    @Test
    public void handleAccountRemovalNoopIfVmPresentInDifferentDC() throws MessageHandlerException {
        DataCenter dc = dcService.getDataCenter(1);
        UUID vmId = UUID.randomUUID();
        VirtualMachineCredit vmCredit = new VirtualMachineCredit(
            orionGuid, 10, 0, 0, "linux", "myh",
            null, "TestShopper", AccountStatus.REMOVED, dc, vmId, false,
            "1", false, 0);
        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);

        callHandleMessage(createTestKafkaMessage("removed"));

        verify(vmZombieService, times(0)).zombieVm(vm.vmId);
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionWhenApiDown() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new ProcessingException(mock(HttpHostConnectException.class))).when(vmZombieService).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        }
        catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionWhenOrchestrationEngineIsDown() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new InternalServerErrorException()).when(vmZombieService).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        }
        catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionWhenApiServiceUnavailable() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new ServiceUnavailableException()).when(vmZombieService).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        }
        catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionIfDBIsDown() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new RuntimeException("Sql.foobar")).when(vmZombieService).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        }
        catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void testHandleMessageCausesTierUpgradePending() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId, 20, 0, "myh");
        callHandleMessage(createTestKafkaMessage("updated"));

        verify(creditServiceMock, times(1)).updateProductMeta(orionGuid, ProductMetaField.PLAN_CHANGE_PENDING, "true");
    }
}
