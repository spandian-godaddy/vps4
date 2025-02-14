package com.godaddy.vps4.handler;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.client.VmService;
import com.godaddy.vps4.web.client.VmShopperMergeService;
import com.godaddy.vps4.web.client.VmSuspendReinstateService;
import com.godaddy.vps4.web.client.VmZombieService;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4AccountMessageHandlerTest {

    private VirtualMachineService vmServiceMock = mock(VirtualMachineService.class);
    private CreditService creditServiceMock = mock(CreditService.class);
    private ActionService vmActionServiceMock = mock(ActionService.class);
    private CommandService commandServiceMock = mock(CommandService.class);
    private DataCenterService dcService = mock(DataCenterService.class);
    private VmZombieService vmZombieServiceMock = mock(VmZombieService.class);
    private VmSuspendReinstateService vmSuspendReinstateService = mock(VmSuspendReinstateService.class);
    private VmService vmService = mock(VmService.class);
    private VmShopperMergeService vmShopperMergeService = mock(VmShopperMergeService.class);
    private Config configMock = mock(Config.class);
    private MessagingService messagingServiceMock = mock(MessagingService.class);
    private VmAction vmAction = mock(VmAction.class);

    private UUID orionGuid;
    private VirtualMachine vm;
    private VirtualMachineCredit vmCredit;

    private final String DEFAULT_TIER = "10";
    private final String DOWNGRADE_TIER = "5";
    private final String UPGRADED_TIER = "20";
    private final String DED4_TIER = "60";
    private final String DEFAULT_UNMANAGED = "0";
    private final String VPS4_MANAGED = "1";
    private final String DED4_MANAGED = "2";
    private final String DEFAULT_CONTROLPANEL = "myh";
    private final String DEFAULT_DATACENTER = "5";
    private Map<String, String> planFeatures;
    private Map<String, String> productMeta;

    @Before
    public void setupTest() {
        planFeatures = new HashMap<>();
        productMeta = new HashMap<>();
        setDefaultPlanFeatures();

        when(dcService.getDataCenter(5)).thenReturn(new DataCenter(5, "testDataCenter"));
        when(configMock.get("vps4.zombie.minimum.account.age")).thenReturn("7");

        orionGuid = UUID.randomUUID();

        ServerSpec vmSpec = new ServerSpec();
        ServerType serverType = new ServerType();
        serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        vmSpec.tier = 10;
        vmSpec.serverType = serverType;

        vm = new VirtualMachine(UUID.randomUUID(),
                                123L,
                                orionGuid,
                                321L,
                                vmSpec,
                                "TestVm",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0,
                                UUID.randomUUID(),
                                null,
                                null);
        when(vmServiceMock.getVirtualMachine(vm.vmId)).thenReturn(vm);

        CommandState command = new CommandState();
        command.commandId = UUID.randomUUID();
        when(commandServiceMock.executeCommand(anyObject())).thenReturn(command);
        when(configMock.get("messaging.reseller.blacklist.fullyManaged", "")).thenReturn("");
    }

    private void setDefaultPlanFeatures() {
        planFeatures.put(PlanFeatures.TIER.toString(), DEFAULT_TIER);
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), DEFAULT_UNMANAGED);
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE.toString(), DEFAULT_CONTROLPANEL);
    }
    private void mockVmCreditWithReseller(AccountStatus accountStatus, UUID productId, String resellerId) {
        if (productId != null) {
            productMeta.put(ProductMetaField.PRODUCT_ID.toString(), vm.vmId.toString());
            productMeta.put(ProductMetaField.DATA_CENTER.toString(), DEFAULT_DATACENTER);
        }

        vmCredit = new VirtualMachineCredit.Builder()
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID("TestShopper")
                .withProductMeta(productMeta)
                .withPlanFeatures(planFeatures)
                .withResellerID(resellerId)
                .build();
        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
    }

    private void mockVmCredit(AccountStatus accountStatus, UUID productId) {
        mockVmCreditWithReseller(accountStatus, productId, "1");
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
                vmZombieServiceMock,
                vmSuspendReinstateService,
                vmService,
                vmShopperMergeService,
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
    public void testHandleMessageAddedCausesNoChange() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, null);
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, times(0)).executeCommand(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testVps4ManagedCredit() throws MessageHandlerException {
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE.toString(), String.valueOf("cpanel"));
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), VPS4_MANAGED);
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        when(messagingServiceMock.sendFullyManagedEmail("TestShopper", "cpanel")).thenReturn("messageId");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");
        Mockito.doNothing().when(creditServiceMock)
                .updateProductMeta(orionGuid, ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");

        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, times(1)).sendFullyManagedEmail("TestShopper", "cpanel");
        verify(creditServiceMock, times(1))
                .updateProductMeta(orionGuid, ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4PlanChange", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testCreditPlidSendsManagedEmail() throws MessageHandlerException {
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), VPS4_MANAGED);
        mockVmCreditWithReseller(AccountStatus.ACTIVE, vm.vmId, "12345");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");
        when(configMock.get("messaging.reseller.blacklist.fullyManaged", "")).thenReturn("4500,495469,527397,525848");

        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock).sendFullyManagedEmail("TestShopper", "myh");
    }

    @Test
    public void testCreditPlidDoesNotSendManagedEmail() throws MessageHandlerException {
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), VPS4_MANAGED);
        mockVmCreditWithReseller(AccountStatus.ACTIVE, vm.vmId, "4500");
        when(configMock.get("messaging.reseller.blacklist.fullyManaged", "")).thenReturn("4500,495469,527397,525848");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");

        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(anyString(), anyString());
    }

    @Test
    public void testDed4DoesNotSendManagedEmail() throws MessageHandlerException {
        planFeatures.put(PlanFeatures.TIER.toString(), DED4_TIER);
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), VPS4_MANAGED);
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(anyString(), anyString());
    }

    @Test
    public void testDed4SendsManagedEmail() throws MessageHandlerException {
        planFeatures.put(PlanFeatures.TIER.toString(), DED4_TIER);
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), DED4_MANAGED);
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("true");
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock).sendFullyManagedEmail("TestShopper", "myh");
    }

    @Test
    public void testDontProcessFullyManagedEmails() throws Exception {
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), VPS4_MANAGED);
        mockVmCredit(AccountStatus.ACTIVE, null);
        when(messagingServiceMock.sendFullyManagedEmail("TestShopper", "cpanel")).thenReturn("messageId");
        when(configMock.get("vps4MessageHandler.processFullyManagedEmails")).thenReturn("false");

        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(messagingServiceMock, never()).sendFullyManagedEmail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testFullyManagedEmailAlreadySent() throws MessageHandlerException {
        VirtualMachineCredit vmCredit = new VirtualMachineCredit.Builder().build();

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
        verify(vmSuspendReinstateService, never()).processSuspend(any(UUID.class));
    }

    @Test
    public void testHandleMessagePurchasedAtNotSet() throws MessageHandlerException {
        when(configMock.get("vps4MessageHandler.primaryMessageConsumerServer")).thenReturn("true");
        mockVmCredit(AccountStatus.ACTIVE, null);
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, times(1))
                .updateProductMeta(eq(orionGuid), eq(ProductMetaField.PURCHASED_AT), any(String.class));
    }

    @Test
    public void testHandleMessagePurchasedAtNotSetNotPrimaryServer() throws MessageHandlerException {
        when(configMock.get("vps4MessageHandler.primaryMessageConsumerServer")).thenReturn("false");
        mockVmCredit(AccountStatus.ACTIVE, null);
        callHandleMessage(createTestKafkaMessage("added"));

        verify(creditServiceMock, never())
                .updateProductMeta(any(UUID.class), any(ProductMetaField.class), any(String.class));
    }

    @Test
    public void testHandleMessagePurchasedAtNotSetNotAddedMessage() throws MessageHandlerException {
        when(configMock.get("vps4MessageHandler.primaryMessageConsumerServer")).thenReturn("true");
        mockVmCredit(AccountStatus.ACTIVE, null);
        callHandleMessage(createTestKafkaMessage("suspended"));

        verify(creditServiceMock, never())
                .updateProductMeta(any(UUID.class), any(ProductMetaField.class), any(String.class));
    }

    @Test
    public void testHandleMessageSuspended() throws MessageHandlerException {
        mockVmCredit(AccountStatus.SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("suspended"));
        when(vmSuspendReinstateService.processSuspend(any(UUID.class))).thenReturn(vmAction);

        verify(vmSuspendReinstateService, times(1)).processSuspend(eq(vm.vmId));
    }

    @Test
    public void testHandleMessageAbuseSuspended() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ABUSE_SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("abuse_suspended"));
        when(vmSuspendReinstateService.processSuspend(any(UUID.class))).thenReturn(vmAction);

        verify(vmSuspendReinstateService, times(1)).processSuspend(eq(vm.vmId));
    }

    @Test
    public void testHandleMessageAbuseSuspendedVmNotFound() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ABUSE_SUSPENDED, UUID.randomUUID());
        callHandleMessage(createTestKafkaMessage("suspended"));

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(0)).executeCommand(argument.capture());
    }

    @Test
    public void testHandleMessageReinstated() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("reinstated"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(vmSuspendReinstateService, times(1)).processReinstate(eq(vm.vmId));
    }

    @Test
    public void testHandleMessageUpgraded() throws MessageHandlerException {
        planFeatures.put(PlanFeatures.TIER.toString(), UPGRADED_TIER);
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("updated"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(vmSuspendReinstateService, never()).processSuspend(eq(vm.vmId));
        verify(creditServiceMock, never()).setStatus(eq(orionGuid), eq(AccountStatus.ABUSE_SUSPENDED));
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        verify(vmService, times(1)).upgradeVm(eq(vm.vmId));
        assertEquals("Vps4PlanChange", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testAbuseSuspendedVmIsNotRenewed() throws MessageHandlerException {
        productMeta.put(ProductMetaField.SUSPENDED.toString(), String.valueOf(true));
        mockVmCredit(AccountStatus.ABUSE_SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("renewed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(vmSuspendReinstateService, never()).processReinstate(eq(vm.vmId));
    }

    @Test
    public void testHandleMessageRenewed() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("renewed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(vmSuspendReinstateService, never()).processReinstate(eq(vm.vmId));
        verify(creditServiceMock, never()).setStatus(eq(orionGuid), eq(AccountStatus.ABUSE_SUSPENDED));
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4PlanChange", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testBillingSuspendedVmCanBeRenewed() throws MessageHandlerException {
        productMeta.put(ProductMetaField.SUSPENDED.toString(), String.valueOf(true));
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("renewed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4PlanChange", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testAbuseSuspendedAccountCanBeRemoved() throws MessageHandlerException {
        productMeta.put(ProductMetaField.SUSPENDED.toString(), String.valueOf(true));
        mockVmCredit(AccountStatus.ABUSE_SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(vmZombieServiceMock, times(1)).zombieVm(eq(vm.vmId));
    }

    @Test
    public void testBillingSuspendedAccountCanBeRemoved() throws MessageHandlerException {
        productMeta.put(ProductMetaField.SUSPENDED.toString(), String.valueOf(true));
        mockVmCredit(AccountStatus.SUSPENDED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(creditServiceMock, times(1)).getVirtualMachineCredit(anyObject());
        verify(vmZombieServiceMock, times(1)).zombieVm(eq(vm.vmId));
    }

    @Test
    public void accountRemovalZombiesAccountWithPurchasedAtNotSet() throws MessageHandlerException {
        productMeta.remove(ProductMetaField.PURCHASED_AT.toString());
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(vmZombieServiceMock).zombieVm(vm.vmId);
    }

    @Test
    public void accountRemovalDeletesAccountWithPurchasedAtLessThan7DaysOld() throws MessageHandlerException {
        Instant sixDaysAgo = Instant.now().minus(6, DAYS);
        productMeta.put(ProductMetaField.PURCHASED_AT.toString(), sixDaysAgo.toString());
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(vmService).destroyVm(vm.vmId);
    }

    @Test
    public void accountRemovalZombiesAccountWithPurchasedAt7DaysOld() throws MessageHandlerException {
        Instant sevenDaysAgo = Instant.now().minus(7, DAYS);
        productMeta.put(ProductMetaField.PURCHASED_AT.toString(), sevenDaysAgo.toString());
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        callHandleMessage(createTestKafkaMessage("removed"));

        verify(vmZombieServiceMock).zombieVm(vm.vmId);
    }

    @Test
    public void handleAccountRemovalNoopIfVmPresentInDifferentDC() throws MessageHandlerException {
        VirtualMachineCredit vmCredit = new VirtualMachineCredit.Builder().build();
        when(creditServiceMock.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);

        callHandleMessage(createTestKafkaMessage("removed"));

        verify(vmZombieServiceMock, times(0)).zombieVm(vm.vmId);
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionWhenApiDown() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new ProcessingException(mock(HttpHostConnectException.class))).when(vmZombieServiceMock)
                .zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        } catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionWhenOrchestrationEngineIsDown() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new InternalServerErrorException()).when(vmZombieServiceMock).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        } catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionWhenApiServiceUnavailable() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new ServiceUnavailableException()).when(vmZombieServiceMock).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        } catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void accountRemovalThrowsRetryableExceptionIfDBIsDown() {
        mockVmCredit(AccountStatus.REMOVED, vm.vmId);
        doThrow(new RuntimeException("Sql.foobar")).when(vmZombieServiceMock).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("removed"));
        } catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void testHandleUnsupportedMessage() throws MessageHandlerException {
        callHandleMessage(createTestKafkaMessage("unsupported_message"));

        verify(creditServiceMock, times(0)).getVirtualMachineCredit(vm.orionGuid);
    }

    @Test
    public void testHandleShopperChangedMessage() throws MessageHandlerException {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        callHandleMessage(createTestKafkaMessage("shopper_changed"));

        verify(vmShopperMergeService, times(1)).mergeShopper(eq(vm.vmId), anyObject());
    }

    @Test
    public void handleShopperChangedThrowsRetryableExceptionIfDBIsDown() {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        doThrow(new RuntimeException("Sql.foobar")).when(vmZombieServiceMock).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("shopper_changed"));
        } catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void handlerSHopperCHangedThrowsRetryableExceptionWhenApiServiceUnavailable() {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        doThrow(new ServiceUnavailableException()).when(vmZombieServiceMock).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("shopper_changed"));
        } catch (MessageHandlerException ex) {
            assertTrue(ex.shouldRetry());
        }
    }

    @Test
    public void handlerShopperChangedThrowsNotRetryableExceptionWhenOrchestrationEngineIsDown() {
        mockVmCredit(AccountStatus.ACTIVE, vm.vmId);
        doThrow(new InternalServerErrorException()).when(vmZombieServiceMock).zombieVm(any(UUID.class));

        try {
            callHandleMessage(createTestKafkaMessage("shopper_changed"));
        } catch (MessageHandlerException ex) {
            assertFalse(ex.shouldRetry());
        }
    }

}
