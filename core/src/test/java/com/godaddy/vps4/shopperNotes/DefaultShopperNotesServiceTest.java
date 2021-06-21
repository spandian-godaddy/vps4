package com.godaddy.vps4.shopperNotes;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultShopperNotesServiceTest {
    private final String resellerId = "1234";
    private final String shopperId = "fake-shopper-id";
    private final UUID shopperNoteId = UUID.randomUUID();

    private DefaultShopperNotesService service;
    private VirtualMachine vm;
    private VirtualMachineCredit credit;

    @Captor private ArgumentCaptor<ShopperNoteRequest> shopperNoteRequestCaptor;
    @Mock private Config config;
    @Mock private ShopperNotesClientService shopperNotesClientService;
    @Mock private CreditService creditService;
    @Mock private VirtualMachineService virtualMachineService;

    @Before
    public void setUp() throws Exception {
        service = new DefaultShopperNotesService(config, shopperNotesClientService,
                                                 creditService, virtualMachineService);
        setUpVm();
        setUpVmCredit();
        setUpMocks();
    }

    private void setUpVm() {
        vm = new VirtualMachine();
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    private void setUpVmCredit() {
        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(vm.orionGuid.toString())
                .withShopperID(shopperId)
                .withResellerID(resellerId)
                .build();
    }

    private void setUpMocks() {
        when(virtualMachineService.getOrionGuidByVmId(vm.vmId)).thenReturn(vm.orionGuid);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);
        when(shopperNotesClientService.processShopperMessage(any(ShopperNoteRequest.class)))
                .thenReturn(shopperNoteId);
        when(config.get("shopper.notes.enteredBy")).thenReturn("vps4-test-name");
        when(config.get("shopper.notes.datetime.pattern")).thenReturn("MM/dd/yyyy HH:mm:ss");
    }

    @Test
    public void processShopperMessageCallsMocks() {
        UUID result = service.processShopperMessage(vm.vmId, "Test shopper note");
        verify(virtualMachineService, times(1)).getOrionGuidByVmId(vm.vmId);
        verify(creditService, times(1)).getVirtualMachineCredit(vm.orionGuid);
        verify(shopperNotesClientService, times(1))
                .processShopperMessage(any(ShopperNoteRequest.class));
        Assert.assertEquals(shopperNoteId, result);
    }

    @Test
    public void basicRequestBodyIsCorrect() {
        service.processShopperMessage(vm.vmId, "Test shopper note");
        verify(shopperNotesClientService, times(1))
                .processShopperMessage(shopperNoteRequestCaptor.capture());
        ShopperNoteRequest request = shopperNoteRequestCaptor.getValue();
        Assert.assertEquals(resellerId, request.plId);
        Assert.assertEquals(shopperId, request.shopperId);
        Assert.assertEquals("vps4-test-name", request.enteredBy);
        Assert.assertEquals("Test shopper note", request.shopperNote);
    }
}
