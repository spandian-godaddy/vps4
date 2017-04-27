package com.godaddy.vps4.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
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

    VirtualMachineService vmServiceMock;
    CreditService creditServiceMock;
    ActionService actionServiceMock;
    Vps4UserService vps4UserServiceMock;
    CommandService commandServiceMock;
    DataCenterService dcService;
    Config configMock;

    @Before
    public void setupTest() {
        vmServiceMock = mock(VirtualMachineService.class);
        creditServiceMock = mock(CreditService.class);
        actionServiceMock = mock(ActionService.class);
        vps4UserServiceMock = mock(Vps4UserService.class);
        commandServiceMock = mock(CommandService.class);
        dcService = mock(DataCenterService.class);
        when(dcService.getDataCenter(5)).thenReturn(new DataCenter(5,"testDataCenter"));
        configMock = mock(Config.class);
        when(configMock.get("nodeping.accountid")).thenReturn("1");
    }

    @Test
    public void handleMessageNoDifferenceTest() throws MessageHandlerException {

        VirtualMachine vm = new VirtualMachine(UUID.randomUUID(), 123L, UUID.fromString("e36b4412-ec52-420f-86fd-cf5332cf0c88"), 321L, null,
                "TestVm", null, null, null,
                null, null, AccountStatus.ACTIVE);
        when(vmServiceMock.getVirtualMachineByOrionGuid(vm.orionGuid)).thenReturn(vm);

        DataCenter dc = dcService.getDataCenter(5);

        VirtualMachineCredit vmCredit = new VirtualMachineCredit(vm.orionGuid, 10, 0, 1, "linux", "none", null, null, "TestShopper", AccountStatus.ACTIVE, dc, vm.vmId);
        when(creditServiceMock.getVirtualMachineCredit(vmCredit.orionGuid)).thenReturn(vmCredit);

        MessageHandler handler = new Vps4MessageHandler(vmServiceMock,
                creditServiceMock,
                actionServiceMock,
                vps4UserServiceMock,
                commandServiceMock,
                configMock);

        handler.handleMessage(
                "{\"id\":\"a82c9629-3e19-4b3a-a870-edc0059eebe5\",\"notification\":{\"type\":[\"added\"],\"account_guid\":\"e36b4412-ec52-420f-86fd-cf5332cf0c88\"}}");

        verify(commandServiceMock, times(0)).executeCommand(anyObject());
    }

    @Test
    public void handleMessageNoVmTest() throws MessageHandlerException {

        VirtualMachine vm = null;
        when(vmServiceMock.getVirtualMachineByOrionGuid(anyObject())).thenReturn(vm);

        MessageHandler handler = new Vps4MessageHandler(vmServiceMock,
                creditServiceMock,
                actionServiceMock,
                vps4UserServiceMock,
                commandServiceMock,
                configMock);

        handler.handleMessage(
                "{\"id\":\"a82c9629-3e19-4b3a-a870-edc0059eebe5\",\"notification\":{\"type\":[\"added\"],\"account_guid\":\"e36b4412-ec52-420f-86fd-cf5332cf0c88\"}}");

        verify(creditServiceMock, times(0)).getVirtualMachineCredit(anyObject());
        verify(commandServiceMock, times(0)).executeCommand(anyObject());
    }

    @Test(expected = MessageHandlerException.class)
    public void handleMessageBadJsonTest() throws MessageHandlerException {
        MessageHandler handler = new Vps4MessageHandler(vmServiceMock,
                creditServiceMock,
                actionServiceMock,
                vps4UserServiceMock,
                commandServiceMock,
                configMock);

        handler.handleMessage(
                "bad json");
    }

    @Test(expected = MessageHandlerException.class)
    public void handleMessageBadValuesTest() throws MessageHandlerException {
        MessageHandler handler = new Vps4MessageHandler(vmServiceMock,
                creditServiceMock,
                actionServiceMock,
                vps4UserServiceMock,
                commandServiceMock,
                configMock);

        handler.handleMessage(
                "{\"id\":\"not a guid\",\"notification\":{\"type\":[\"added\"],\"account_guid\":\"e36b4412-ec52-420f-86fd-cf5332cf0c88\"}}");
    }

    @Test
    public void handleMessageRemovedTest() throws MessageHandlerException {

        VirtualMachine vm = new VirtualMachine(UUID.randomUUID(), 123L, UUID.fromString("e36b4412-ec52-420f-86fd-cf5332cf0c88"), 321L, null,
                "TestVm", null, null, null,
                null, null, AccountStatus.ACTIVE);
        when(vmServiceMock.getVirtualMachineByOrionGuid(vm.orionGuid)).thenReturn(vm);

        DataCenter dc = dcService.getDataCenter(5);

        VirtualMachineCredit vmCredit = new VirtualMachineCredit(vm.orionGuid, 10, 0, 1, "linux", "none", null, null, "TestShopper",
                AccountStatus.REMOVED, dc, vm.vmId);
        when(creditServiceMock.getVirtualMachineCredit(vmCredit.orionGuid)).thenReturn(vmCredit);

        Vps4User user = new Vps4User(123, vmCredit.shopperId);
        when(vps4UserServiceMock.getUser(vmCredit.shopperId)).thenReturn(user);

        CommandState command = new CommandState();
        command.commandId = UUID.randomUUID();
        when(commandServiceMock.executeCommand(anyObject())).thenReturn(command);
        doNothing().when(actionServiceMock).tagWithCommand(anyLong(), anyObject());

        MessageHandler handler = new Vps4MessageHandler(vmServiceMock,
                creditServiceMock,
                actionServiceMock,
                vps4UserServiceMock,
                commandServiceMock,
                configMock);

        handler.handleMessage(
                "{\"id\":\"a82c9629-3e19-4b3a-a870-edc0059eebe5\",\"notification\":{\"type\":[\"added\"],\"account_guid\":\"e36b4412-ec52-420f-86fd-cf5332cf0c88\"}}");

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandServiceMock, times(1)).executeCommand(argument.capture());
        assertEquals("Vps4DestroyVm", argument.getValue().commands.get(0).command);
    }

}
