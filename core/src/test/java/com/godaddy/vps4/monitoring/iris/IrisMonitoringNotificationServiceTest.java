package com.godaddy.vps4.monitoring.iris;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.monitoring.iris.irisClient.CreateIncidentInput;
import com.godaddy.vps4.monitoring.iris.irisClient.IrisWebServiceSoap;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

public class IrisMonitoringNotificationServiceTest {

    @Test
    public void sendServerDownEventNotificationTest() {
        Config configMock = mock(Config.class);
        when(configMock.get("monitoring.iris.group.id")).thenReturn("583");

        VirtualMachineService vmServiceMock = mock(VirtualMachineService.class);
        when(vmServiceMock.getUserIdByVmId(any(UUID.class))).thenReturn(1L);

        Vps4UserService userServiceMock = mock(Vps4UserService.class);
        Vps4User testUser = new Vps4User(1, "yl9", "1");
        when(userServiceMock.getUser(anyLong())).thenReturn(testUser);

        IrisWebServiceSoap irisWebServiceSoapMock = mock(IrisWebServiceSoap.class);
        when(irisWebServiceSoapMock.createIrisIncident(anyObject())).thenReturn(123L);

        IrisMonitoringNotificationService service = new IrisMonitoringNotificationService(configMock, vmServiceMock,
                userServiceMock, irisWebServiceSoapMock);

        VirtualMachine vm = new VirtualMachine();
        vm.hostname = "TestHostname";
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.hfsVmId = 123L;
        vm.primaryIpAddress = new IpAddress();
        vm.primaryIpAddress.ipAddress = "127.0.0.1";

        long irisTicketId = service.sendServerDownEventNotification(vm);

        ArgumentCaptor<CreateIncidentInput> captor = ArgumentCaptor.forClass(CreateIncidentInput.class);
        
        assertEquals(123L, irisTicketId);
        verify(irisWebServiceSoapMock, times(1)).createIrisIncident(captor.capture());
        CreateIncidentInput input = captor.getValue();
        assertEquals(input.getGroupId(), 583);
    }
}
