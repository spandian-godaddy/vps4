package com.godaddy.vps4.web.controlPanel;

import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;

import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class ControlPanelRequestValidationTest {

    VirtualMachine vm;
    Image image;

    @Before
    public void setUp() {
        image = Mockito.mock(Image.class);
        image.controlPanel = ControlPanel.PLESK;

        vm = new VirtualMachine(
                UUID.randomUUID(), //id
                1234,  //vmId
                UUID.randomUUID(), //orionGuid
                1, //projectId
                null, //VirtualMachineSpec
                "fakeName", //name
                image, //Image
                null, //IpAddress
                null, //validOn
                null, //validUntil
                "omg.host.name", //hostname
                AccountStatus.ACTIVE //status
                );
    }

    @Test
    public void testVmExists() {
        ControlPanelRequestValidation.validateVmExists(vm, UUID.randomUUID());
    }

    @Test(expected=NotFoundException.class)
    public void testVmDoesntExist() {
        ControlPanelRequestValidation.validateVmExists(null, UUID.randomUUID());
    }

    @Test
    public void testCorrectControlPanel() {
        ControlPanelRequestValidation.validateCorrectControlPanel(vm, ControlPanel.PLESK, UUID.randomUUID());
    }

    @Test(expected=Vps4Exception.class)
    public void testIncorrectControlPanel(){
        ControlPanelRequestValidation.validateCorrectControlPanel(vm, ControlPanel.CPANEL, UUID.randomUUID());
    }

    @Test
    public void testGetValidVirtualMachine() {
        Vps4User user = new Vps4User(1, "fakeShopper");
        PrivilegeService privilegeService = Mockito.mock(PrivilegeService.class);

        VirtualMachineService virtualMachineService = Mockito.mock(VirtualMachineService.class);
        Mockito.when(virtualMachineService.getVirtualMachine(Mockito.any(UUID.class))).thenReturn(vm);

        VmService hfsVmService = Mockito.mock(VmService.class);
        Vm hfsVm = new Vm();
        hfsVm.status = "ACTIVE";
        Mockito.when(hfsVmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);

        ControlPanelRequestValidation.getValidVirtualMachine(user, privilegeService,
                virtualMachineService, hfsVmService, ControlPanel.PLESK, UUID.randomUUID());
    }

}
